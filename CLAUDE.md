# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=GameSmokeTest

# Run a single test method
mvn test -Dtest=GameSmokeTest#testRollSmoke

# Build without tests
mvn clean package -DskipTests
```

**Running the app:** Launch `StartMonopolyApp.java` from an IDE (IntelliJ). No packaged release build exists — the Processing desktop renderer requires IDE execution.

## Architecture Overview

This is a Java 19 / Processing 4 Monopoly board game undergoing incremental extraction toward a backend-ready client/server split. The architecture is layered, with each layer increasingly "clean":

```
domain        → pure immutable state records (SessionState, TurnState)
application   → command handlers and gateways that mutate domain state
host          → session lifecycle, bot scheduling, persistence
presentation  → coordinators that project session state to UI
components    → legacy Processing-era runtime objects (Game, Players, Board, etc.)
client        → MonopolyApp (PApplet), desktop shell, frame loop
```

**SessionState** (a Java 19 immutable record) is the authoritative game truth. Commands flow in via `SessionApplicationService`, which routes them to typed command handlers (e.g. `PropertyPurchaseCommandHandler`). The resulting new `SessionState` is projected to the UI by `GameSessionStateCoordinator`.

**Gateways** bridge the command/state model to legacy runtime objects. For example, `PropertyPurchaseGateway` lets command handlers mutate legacy `Players`/`Game` state without the application layer depending on those classes directly.

**Coordinators** decompose what used to be a God-object `Game`. The main ones:
- `GameTurnFlowCoordinator` — turn progression logic
- `GameSessionStateCoordinator` — projects SessionState changes to the sidebar/UI
- `GameDesktopPresentationCoordinator` — desktop frame lifecycle and UI hooks
- `GameBotTurnControlCoordinator` — schedules and steps bot player turns
- `GameDesktopSessionCoordinator` — session bridge, save/load, reattachment

**The frame loop:** `MonopolyApp.draw()` → `DesktopAppShell.advanceFrame()` → coordinators advance → current `DesktopSessionRenderView` renders to the Processing canvas.

## Key Architectural Rules

- **Do not add new gameplay authority into `Game.java` or `PopupService.java`.** New rule logic belongs in command handlers, gateways, or domain types.
- `fi.monopoly.components.Game` is a controlled legacy seam — it still holds compatibility-heavy runtime objects but is no longer where wiring lives.
- `SessionState` is always immutable; commands return new instances rather than mutating.
- Commands are routed through `SessionApplicationService` with typed dispatch — find the right handler rather than adding ad-hoc mutation.

## Testing

Tests are in `src/test/java/fi/monopoly/`. Key test types:

- **Headless smoke/simulation tests** (`GameSmokeTest`, `GameBotSimulationTest`) — drive the full Processing game loop without a window using `TestDesktopRuntimeFactory`. These catch regressions where the game stalls in animation/popup/auction/debt states.
- **Flow tests** (`GameBankruptcyTest`, `GameDebtTest`, `GameTurnControlsTest`) — validate specific subsystem flows end-to-end.
- **Unit tests** — for domain, application, board, turn engine, etc.

Headless tests use synthetic key presses to advance the game and assert that it doesn't deadlock within a turn/roll budget.

## Migration Direction

The project is mid-way through extracting a backend-ready architecture. The current branch `separation-program` continues this work. The docs in `docs/` describe the plan and status:

- `docs/current-architecture-status.md` — what is done, what is still legacy, main blockers
- `docs/backend-ready-fast-track-plan.md` — next extraction steps
- `docs/architecture-overview-diagrams.md` — structural diagrams

The next priorities are: (1) introduce a client-facing session host interface, (2) move bot ownership fully behind it, (3) separate client-local runtime reconstruction from authoritative session execution. The `separation-program` branch is where this work lands.

## Working Rules

### Before making changes
- If a request is ambiguous, ask clarifying questions first — do not assume missing requirements.
- Identify relevant files and briefly explain what will change and why before touching code.
- Always preserve existing behavior unless explicitly told otherwise.
- Keep README and local docs in sync when behavior, controls, setup, or UI capabilities change.

### Suggestions vs. implementation
Suggest improvements separately from implementation. Do not implement improvements unless explicitly approved. Suggestions may include: code structure, performance, naming, suitable 3rd party Java libraries (explain why; avoid heavy dependencies unless clearly justified).

### Testing
- Missing tests for changed logic → create them.
- Existing tests → update if needed.
- Bug fix → add a test that would have caught it (if reasonable).
- UI layout/positioning-only changes → skip tests.

### UI layout
- No overlapping UI elements. Use 8px or 16px spacing grid. Maintain alignment and visual consistency.
- Window resizing is enabled, but board layout is still fixed-size — preserve a safe minimum window size until board scaling is implemented.
- **All UI/layout numbers go into `LayoutMetrics` / `UIToken` classes. Never hardcode pixel values in component code.**

### TODO.txt
Maintain `TODO.txt` at the project root:
- Assign priority (High / Medium / Low) to every item.
- Remove completed items.
- Suggest new items when discovering missing features or improvements.
- Keep entries aligned with current implementation status.

### Monopoly rules
Follow the official rules at https://ristiseiska.fi/monopoly/. If a request conflicts with those rules, do not implement it — explain the conflict and suggest alternatives.

## Tech Stack

| | |
|---|---|
| Language | Java 19 |
| UI framework | Processing 4 (via JitPack), ControlP5 |
| Build | Maven 3.9 |
| Tests | JUnit 5 |
| Utilities | Lombok, Logback, Jackson |
| Locales | Finnish (`fi`) and English (`en`) — card decks in `src/main/resources/` |