# Current Architecture Status

## Purpose

This document is the current-state companion to the original separation-program docs.

Use it when you want to know:

- what has already been implemented in the current codebase
- what is still legacy or transitional
- what the main blockers are before backend extraction
- which architectural seams are already good enough to build on

This is not the original plan. This is the status snapshot of the repo as it exists now.

## Executive Summary

The project is no longer in the early local-separation stage.

The codebase already has:

- authoritative session-oriented application services
- extracted desktop assembly, shell, runtime, session, and UI packages
- bot, turn, and session presentation coordinators separated from the old `Game` god object
- local save/load through session-oriented seams
- runtime reattachment support for restored local sessions

The project does not yet have full backend-ready architecture because:

- the desktop client still owns the authoritative application service instance locally
- legacy runtime objects are still rebuilt and mutated in-process
- `Game` is still a compatibility-heavy desktop host for tests and current orchestration
- the desktop shell still contains local-only coordination that should become client/server seams later
- the client still renders a host-provided live view in-process rather than a transport-neutral view model

## Current Implemented Shape

### What is already separated

- `client.desktop`
  - Processing app-facing shell/runtime adapters, runtime resources, explicit client-global desktop settings, and narrow rendering/runtime seams around the embedded local client session
  - desktop app bootstrap now also consumes one explicit client-host binding instead of assembling the embedded-local host shell inline inside `DesktopAppShell`
- `host.session.local`
  - embedded host-owned session lifecycle, persistence, snapshot publication, hosted-game lifecycle, and test-access seams for the in-process desktop mode
  - embedded host-owned local game loop coordination now also drives bot stepping outside the presentation frame coordinator
- `presentation.game.desktop.session`
  - session bridge and restored-session reattachment adapters around the embedded host
- `client.session.desktop`
  - desktop client-session shell/runtime glue, client-owned local session control callbacks such as save/load triggers, and the app-facing desktop session runtime port
  - client-facing `ClientSessionUpdates` now carries the transport-neutral session update stream, while host-only restore authority stays behind `SessionHost`
  - the old generic `ClientSession` type has now been removed; the client depends explicitly on a listener-based session-update gateway instead of a host-shaped session object
  - embedded desktop frame advancement now also crosses a dedicated local frame-driver seam instead of living on the client session-update gateway
  - fresh local session creation, local save/load, and persistence notices now also live behind a dedicated desktop-local session controls port instead of the transport-neutral session-update gateway
  - embedded desktop live render access now also crosses a dedicated local view port instead of living on the session-update gateway
  - the live render view type itself now also lives under `client.session.desktop` instead of the transport-neutral `client.session` package
  - the desktop app shell now owns a small client-side session model fed by `ClientSessionUpdates` listener updates, so snapshot state no longer reaches the app by raw runtime pass-through getters/listener registration
  - the desktop app shell now also owns a small client-side render model, so the runtime port no longer exposes raw live-view polling directly to the app
  - `SessionCommandPort` now defines the transport-neutral command submission and state query seam; presentation-layer session adapters (debt, auction, purchase, trade) depend on `SessionCommandPort` instead of the full `SessionApplicationService`
  - `BotTurnScheduler` no longer imports `DesktopClientSettings` directly; `skipAnimations` is now injected as a `BooleanSupplier` at construction time
  - `GameSessionStateCoordinator.onDebtStateChanged()` no longer takes `SessionApplicationService` directly; `clearDebtOverride` is passed as a `Runnable` callback
  - `SessionPresentationStatePort` introduced in `application.session` for the legacy override-state operations; `GameDesktopShellDependencies.StateAccess` now uses two narrow typed suppliers (`SessionCommandPort` + `SessionPresentationStatePort`) so shell and session coordinators no longer import `SessionApplicationService`
  - `GamePresentationFactory.Dependencies` now holds `SessionCommandPort` + narrow callbacks instead of `SessionApplicationService`
  - `SessionApplicationService` eliminated from `Game`, all assembly tunnel records (`GameDesktopAssembly`, `GameDesktopHostContext`), and `GameSessionBridge`; replaced with narrow port types
  - `SessionPaymentPort` introduced in `application.session` for the `handlePaymentRequest` seam
  - `SessionApplicationService` now only imported in `GameSessionBridgeFactory` (internal use) and `LegacySessionApplicationFactory` (construction)
- `domain.session`
  - authoritative session records and continuation state
- `application.session`
  - session-oriented orchestration and persistence-facing services
- `host.bot`
  - host-owned bot scheduling, bot turn stepping, and embedded local bot command adapters
  - desktop-local popup/trade/projected-view access now enters host bot flow through one explicit interaction adapter seam instead of direct runtime/controller references
- `presentation.game.turn`
  - turn flow orchestration
- `presentation.game.session`
  - session state projection helpers and queries
- `presentation.game.desktop.assembly`
  - desktop object graph assembly
- `presentation.game.desktop.shell`
  - host-to-coordinator orchestration
- `presentation.game.desktop.runtime`
  - lifecycle/bootstrap/debug coordination for local runtime
- `presentation.game.desktop.session`
  - session bridge, restored-session reattachment
- `presentation.game.desktop.ui`
  - frame/layout/input/presentation host responsibilities
  - dedicated UI session-controls port now isolates pause/bot-speed/language/save-load actions from broader presentation hooks

### Important current bridge classes

These are still transitional and should be treated as controlled legacy seams:

- `fi.monopoly.components.Game`
- `fi.monopoly.client.desktop.MonopolyApp`
- `fi.monopoly.client.desktop.MonopolyRuntime`
- `fi.monopoly.presentation.game.desktop.shell.GameDesktopSessionCoordinator`
- `fi.monopoly.presentation.game.desktop.shell.GameDesktopPresentationCoordinator`
- `fi.monopoly.presentation.game.desktop.session.GameSessionBridgeFactory`
- `fi.monopoly.presentation.game.desktop.runtime.GameRuntimeAssemblyFactory`
- `fi.monopoly.presentation.session.*` gateway/adapter classes

### Meaning of `Game` today

`Game` is no longer the primary place where gameplay wiring lives.

It is now mainly:

- the desktop host/compatibility surface
- the holder of current Processing-era live objects
- the entry point still expected by current tests and app bootstrap

That is much better than before, but it is still not the shape we want for a backend-ready client.

## What Is Still Too Local

### 1. Authoritative session host is still owned by the desktop process

Today the client still constructs and owns the local authoritative session service graph.

Backend-ready target:

- client talks to a session host
- session host owns command execution and bot scheduling
- client no longer creates the authoritative game service graph directly

### 2. Legacy runtime mutation still happens in-process

Today the same process still contains:

- authoritative session state
- legacy runtime objects
- UI rendering/runtime concerns

Backend-ready target:

- legacy runtime becomes a pure client-side rendering/runtime projection
- authoritative mutation stays outside the Processing host

### 3. Desktop shell boundaries are still optimized for local mode

The current shell is much cleaner than before, and the old single `GameDesktopShellCoordinator`
has already been split into explicit session and presentation coordinators. Some interfaces still
bundle:

- local runtime concerns
- session commands/state access
- client-only presentation visibility/interactivity rules

Backend-ready target:

- client command transport boundary becomes explicit
- local runtime rebuild/reattachment becomes a client concern only
- server/session-host concerns become separate package roots

The recent `client.desktop` and embedded-host moves improved this:

- `MonopolyApp` and `MonopolyRuntime` are now explicitly on the client side of the architecture
- the root package no longer acts as the owner of desktop bootstrap/runtime state
- future extraction work can now target `client.desktop` directly instead of peeling adapter classes out of `fi.monopoly`
- host tick advancement and client render access are also now explicitly different seams, which is closer to the eventual client/host split even in embedded mode
- embedded local bot stepping now runs through a host-owned loop coordinator instead of being scheduled from the presentation frame coordinator
- host-owned bot turn contexts now request projected game/player views for the actual acting player, which removes a local desktop assumption that leaked current-turn projections into debt resolution
- desktop UI session controls now cross the shell boundary through one dedicated `GameUiSessionControls` port instead of being mixed into the broader gameplay presentation hook surface
- embedded local hosting now also talks to a dedicated `DesktopHostedGame` adapter around `Game` instead of depending on `Game` itself as the hosted-session interface
- the embedded host now exposes the client-facing session seam directly, so the extra `LocalDesktopClientSession` forwarding adapter is no longer in the path
- compatibility-heavy test inspection on `Game` is now also being centralized behind one explicit test facade instead of scattered private reflection hooks
- desktop app-shell persistence/runtime helpers now also receive the active runtime explicitly from `DesktopRuntimeBridge` instead of reading it through the global runtime singleton
- `DesktopAppShell` now also depends on a bundled client-host binding plus narrow `DesktopRuntimeAccess` port instead of directly owning the embedded local host shell/bootstrap graph
- desktop input/event dispatch now also reads the active event bus through the app-shell/runtime seam instead of a global runtime lookup inside the Processing observer base class
- `MonopolyApp` no longer installs a placeholder global runtime during construction; the desktop runtime now appears only from explicit bootstrap/test initialization paths
- shared rendering helpers no longer depend on a global current-app context; callers now pass an explicit rendering context instead
- street/utility property runtime-dependent checks now also resolve through the owning player's runtime/session context instead of reading the global runtime singleton

### 4. Tests still lean on local host internals

Many current tests still use package-visible access to:

- `players()`
- `dices()`
- `animations()`
- `getBoard()`
- `debtController()`

This is acceptable for now, but it means `Game` still carries compatibility load that a backend-ready client would not need.

## What Is Already Good Enough For Backend Extraction

These are the strongest existing assets:

### 1. Session-oriented application layer

The project already has a meaningful application/service seam around authoritative session state.

That is the most important prerequisite for backend extraction.

### 2. Split presentation coordination

Turn flow, bot flow, and desktop presentation responsibilities are no longer collapsed into one class.

That makes it realistic to:

- move bot execution to a host process
- keep rendering on the client
- keep current desktop app working while doing it

### 3. Save/load and continuation groundwork

Local save/load is already much closer to backend-style snapshot restoration than before.

This reduces the amount of “special client-only restore logic” that would otherwise block backend work.

### 4. Explicit desktop assembly/runtime/session packages

The package structure now already hints at the future split:

- local client assembly/runtime
- session bridge/host seams
- presentation-only UI packages

That is a strong staging point for introducing actual backend/client package roots.

## Main Remaining Technical Blockers

These are the blockers that matter most before the project is truly backend-ready.

### Blocker A: the client session-update gateway — RESOLVED

The client now has three complementary transport-neutral seams:

- `ClientSessionUpdates` — listener-only snapshot stream (receive snapshots from host)
- `SessionCommandPort` — command submission and state query (send commands to host)
- `ClientSessionSnapshot` — now carries the full `SessionState` alongside session metadata

`ClientSessionSnapshot` was intentionally small initially (just `sessionId`, `version`, `status`,
`viewAvailable`). It now also carries the complete authoritative `SessionState`. This means:
- any host implementation (embedded or remote) can push a self-sufficient snapshot
- the client can reconstruct its local presentation model from the received snapshot
- a remote transport MVP can serialize and push `ClientSessionSnapshot` over the wire directly

`DesktopClientSessionModel.sessionState()` exposes the current session state for any client code
that needs to drive its presentation from host-approved state rather than polling live runtime objects.

Presentation-layer adapters (debt, auction, purchase, trade) depend on `SessionCommandPort`
instead of the full `SessionApplicationService`. `SessionApplicationService` implements
`SessionCommandPort`, so the embedded local mode works without any behavioral change.

The current seam should evolve cleanly into something that works for both:

- local embedded host (current: `SessionApplicationService` implements `SessionCommandPort`)
- remote backend host (future: transport adapter implements `SessionCommandPort`)

### Blocker B: bot ownership is only partially host-owned so far

Bot coordination is cleaner now, and embedded local mode already routes bot stepping through a host-owned loop.
The concrete bot collaborators live in `host.bot` (with `DesktopHostBotInteractionAdapter` as the only
desktop-specific implementation in `presentation.game.desktop.assembly`).

Additional narrowing steps now in place:

- `BotTurnScheduler` no longer imports `DesktopClientSettings`; `skipAnimations` is injected as `BooleanSupplier`
- `host.bot` no longer reaches directly into desktop runtime popup services, trade controllers, or turn-player-only projected view suppliers
- those dependencies now cross the boundary through a dedicated desktop interaction adapter
- `SessionBackedComputerTurnContext` now depends on `SessionCommandPort` instead of `SessionApplicationService` — all bot command submissions and state queries go through the transport-neutral interface
- `GameBotTurnHooksAdapter` also takes `SessionCommandPort` for the main command flow; the computer auction action (a specialized application-layer behavior) is injected as a `Function<String, CommandResult>` lambda rather than requiring the full service type

Backend-ready target:

- bot loop belongs to the host
- client only renders resulting snapshots

### Blocker C: legacy bridge still sits between app and runtime in the same process

Progress since last update:
- `OverlaySessionStateStore` introduced in `application.session`: encapsulates the five mutable
  flow-state fields (`pendingDecision`, `auctionState`, `activeDebt`, `tradeState`,
  `turnContinuationState`) that previously lived as instance variables on `SessionApplicationService`
- `SessionApplicationService.currentState()` is now a plain `overlay.get()` call — turn phase
  derivation and stale pending-decision clearing happen inside the store's `get()` method
- `SessionApplicationService` keeps its convenience `Supplier<SessionState>` constructor, so
  `LegacySessionApplicationFactory` and existing tests need no changes; the overlay store is
  created internally from the supplier



This is the biggest structural bridge still left.

Progress made:

- `EmbeddedDesktopSessionHost` now implements `SessionCommandPort`, making it the single named
  entry point for both command submission and snapshot reception
- `HostedLocalSession` explicitly extends `SessionCommandPort`, so any future host implementation
  must satisfy both halves of the client-facing seam
- presentation-layer adapters already depend on `SessionCommandPort` — they can be rewired to
  the host entry point without behavior change once the assembly is restructured
- `SessionPresentationStatePort` introduced for the legacy override-state operations
  (`hasAuctionOverride`, `hasTradeOverride`, `hasPendingDecisionOverride`, `clearActiveDebtOverride`,
  `restoreFrom`); shell coordinators, `GameSessionStateCoordinator`, and
  `RestoredSessionReattachmentCoordinator` no longer import `SessionApplicationService`
- `GameDesktopShellDependencies.StateAccess` now holds two narrow suppliers (`SessionCommandPort`
  and `SessionPresentationStatePort`) instead of one `SessionApplicationService` supplier
- `GamePresentationFactory.Dependencies` now holds `SessionCommandPort` + explicit
  `Consumer<TurnContinuationGateway>` + `Function<String,CommandResult>` instead of the full
  application service; `GamePresentationFactory` no longer imports `SessionApplicationService`
- `SessionApplicationService` removed from `Game`, `GameDesktopAssembly`, `GameDesktopHostContext`,
  and `GameSessionBridge` record — these now hold only narrow types: `SessionCommandPort`,
  `SessionPresentationStatePort`, `SessionPaymentPort`, `Consumer<TurnContinuationGateway>`,
  `Function<String,CommandResult>`, and `internalCommandPort` (a `SessionCommandPort` back-channel
  that `Game.submitCommand()` uses to avoid routing through the proxy)
- `SessionPaymentPort` introduced in `application.session` for the `handlePaymentRequest` seam;
  `Game` no longer imports `SessionApplicationService` at all
- `SessionApplicationService` is now only imported in: `GameSessionBridgeFactory` (where it is
  used internally to build the bridge) and `LegacySessionApplicationFactory` (where it is built)
- no assembly record above the bridge factory level references `SessionApplicationService` by name
- `ForwardingSessionCommandPort` introduced in `client.session`: a mutable proxy that holds a
  separate `stateSource` (always points to the local `SessionApplicationService` for projected state)
  and a `commandDelegate` (initially `SessionApplicationService`, rewired to
  `EmbeddedDesktopSessionHost` after session start via `setExternalCommandDelegate`)
- all five presentation-layer adapters (debt, auction, purchase, trade, trade controller) now
  receive the `ForwardingSessionCommandPort` proxy, not `SessionApplicationService` directly —
  their command submissions route through `EmbeddedDesktopSessionHost.handle()` transparently
- `EmbeddedDesktopSessionHost.handle()` now publishes a snapshot directly after each accepted
  command; `SessionApplicationService.postCommandListener` and `setPostCommandListener()` removed
- `DesktopHostedGame.setExternalCommandDelegate()` replaces `setPostCommandListener()` — the host
  calls this after game creation to wire the proxy delegate to itself
- `EmbeddedDesktopSessionHost` is now the single named command entry point for both embedded and
  future remote modes; swapping to a remote host only requires changing what the proxy delegates to

Remaining:

- session host output must become client-facing session state/view state
- legacy runtime reconstruction must become a client adapter

### Blocker D: server.transport HTTP MVP — DONE

`fi.monopoly.server.transport` now exists with:

- `SessionCommandMapper` — deserializes JSON (with `"type"` discriminator) to typed
  `SessionCommand` instances; all 22 command types covered; no Jackson annotations on domain types
- `SessionHttpServer` — built-in Java `HttpServer` exposing:
  - `POST /command` → `SessionCommandPort.handle()`, returns `{"accepted":…,"rejections":[…]}`
  - `GET /snapshot` → serialized `ClientSessionSnapshot` JSON
  - `GET /health` → `{"status":"ok"}`

The HTTP server is wired into `EmbeddedLocalDesktopClientBindingFactory` behind
`-Dmonopoly.http.port=<port>`. When that system property is set, the embedded session host is
exposed over HTTP on the given port; a JVM shutdown hook stops the server cleanly. When the
property is absent the app runs in normal embedded-only mode with no behavioral change.

`EmbeddedDesktopSessionHost.currentSnapshot()` is now public so the HTTP server (and other
transport implementations) can poll it directly.

The full transport layer MVP is now in place:

- `SessionCommandSerializer` — serializes `SessionCommand` → JSON (symmetric with `SessionCommandMapper`)
- `HttpSessionCommandPort` — `SessionCommandPort` that POSTs commands to `/command`
- `HttpClientSessionUpdates` — `ClientSessionUpdates` that connects to `/events` SSE stream,
  auto-reconnects on disconnect, and dispatches received snapshots to registered listeners

A remote desktop client can substitute `HttpSessionCommandPort` + `HttpClientSessionUpdates` for the
embedded host binding without any changes to the five presentation-layer adapters — they only
see `SessionCommandPort` and `ClientSessionUpdates`.

Progress since HTTP MVP:
- `PendingDecision.payload` is now typed as the sealed interface `DecisionPayload` (domain package)
  with `@JsonTypeInfo` / `@JsonSubTypes` so Jackson round-trips the type through HTTP without
  transport-layer MixIns; `PropertyPurchaseDecisionPayload implements DecisionPayload`
- `server.session` package created: `SessionServer` wraps `SessionHttpServer` lifecycle (start,
  stop, shutdown hook) and is used by `EmbeddedLocalDesktopClientBindingFactory`; `StartSessionServer`
  documents the future standalone `main()` entry point and the remaining gateway extraction work
- Java upgraded to 21; `SessionHttpServer` uses `Executors.newVirtualThreadPerTaskExecutor()`;
  SSE reader uses `Thread.ofVirtual()`; `SessionCommandSerializer` and
  `InteractiveTurnEffectExecutor` use Java 21 pattern-matching switch statements

Remaining for full backend split (main blocker):
- `server.session` — standalone server process requires pure domain gateway implementations
  (currently all gateways in `presentation.legacy.session.*` depend on Processing-era mutable
  runtime objects: `Players`, `Dices`, `DebtController`, etc.)
- extracting rule logic (rent, movement, jail, auction bidding, building sale) from legacy
  runtime objects into the domain/application layer is the prerequisite for standalone operation

## Recommended Immediate Architectural Focus

Blockers A, B (partially), C (partially), and D are resolved. The main remaining work before full
standalone server operation:

1. **Extract rule logic from legacy runtime into domain/application layer** — rent calculation,
   movement, jail handling, auction bidding, building sale must move to pure Java code before the
   gateway adapters in `presentation.legacy.session.*` can be replaced
2. **Replace legacy gateway adapters** — once rule logic is in the domain, create pure implementations
   of `AuctionGateway`, `DebtRemediationGateway`, `PropertyPurchaseGateway`, `TradeGateway`,
   `TurnActionGateway` that do not depend on `Players`, `Game`, or `PopupService`
3. **`SessionCommandPublisher`** — DONE: `SessionCommandPublisher` in `server.session` decorates
   any `SessionCommandPort`, publishes snapshots to registered `ClientSessionListener`s after each
   accepted command, and exposes `currentSnapshot()` for use as the snapshot supplier seam;
   `StartSessionServer` now only needs `PureDomainSessionFactory` before it can run
4. **Make desktop client render from snapshot** — `Game` and the legacy Processing runtime should
   become a pure client-side rendering projection that reads from received `SessionState` rather
   than computing authoritative values themselves

## Relationship To Older Plan Docs

The older PR1-PR12 docs are still useful as design history and scope references.

But they should now be read as:

- original migration intent
- subsystem design notes
- not the exact current implementation status

For current truth, prefer:

1. this document
2. `architecture-overview-diagrams.md`
3. the new backend-fast-track plan
