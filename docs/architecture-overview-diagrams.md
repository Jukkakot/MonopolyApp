# Architecture Overview Diagrams

## Purpose

This file is the fast visual companion to the longer architecture docs.

Use it when you want a quick answer to:

- what the current app architecture roughly looks like
- what the current local-first separated shape looks like
- what the intended backend-ready target shape looks like

The diagrams are intentionally high-level. They are not a class diagram and they do not try to show every adapter.

## 1. Before Separation

This is the legacy shape the project started from conceptually.

```mermaid
flowchart LR
    UI[Processing UI / ControlP5 / Popup Widgets]
    GAME[Game god object]
    RUNTIME[MonopolyRuntime singleton]
    RULES[Mixed rules / turn flow / property logic]
    BOTS[Bot logic]
    UI --> GAME
    GAME --> RUNTIME
    GAME --> RULES
    GAME --> BOTS
    RULES --> UI
    RULES --> RUNTIME
    BOTS --> UI
```

What this means:

- the UI, orchestration, and rule authority were too mixed together
- popup flow was often effectively gameplay flow
- bot execution depended too much on client/runtime assumptions

## 2. Current Structure In This Repo

This is the rough shape now after the local separation work currently implemented.

```mermaid
flowchart LR
    subgraph DesktopHost[Desktop Host]
        GAME[Game host]
        HOSTFACTORY[GameDesktopHostFactory]
        HOST[DesktopSessionHostCoordinator]
        LOCALACTIONS[LocalSessionActions]
    end

    subgraph DesktopPresentation[Desktop Presentation]
        CLIENTAPP[client.desktop]
        ASSEMBLY[desktop.assembly]
        SHELL[desktop.shell]
        UI[desktop.ui]
        VIEW[desktop.session.SessionViewFacade]
        REATTACH[RestoredSessionReattachmentCoordinator]
    end

    subgraph DesktopRuntime[Legacy Runtime Shell]
        RUNTIMEFACTORY[GameRuntimeAssemblyFactory]
        BOOTSTRAP[LegacyGameRuntimeBootstrapper]
        LEGACY[Players / board / property runtime]
        DEBUG[DebugController]
    end

    subgraph TurnBotSession[Presentation Game Coordination]
        TURN[turn.GameTurnFlowCoordinator]
        BOT[bot.*]
        SESSIONPRESENT[session.GameSessionStateCoordinator]
        QUERIES[session.GameSessionQueries]
    end

    subgraph Application[Application Layer]
        APP[SessionApplicationService]
        APPFACTORY[LegacySessionApplicationFactory]
        PURCHASE[Property purchase flow]
        DEBT[Debt / debt dispatch]
        AUCTION[Auction adapters]
        TRADE[Trade adapters]
        PERSIST[SessionPersistenceService]
        LOCALPERSIST[LocalSessionPersistenceCoordinator]
    end

    subgraph PersistenceInfra[Persistence Infrastructure]
        INFRA[fi.monopoly.infrastructure.*]
        STORE[SessionSnapshotStore]
        JSON[JsonFileSessionSnapshotStore]
    end

    subgraph Domain[Authoritative State]
        SESSION[SessionState]
        TURN[TurnState / TurnContinuationState]
        SUBSYSTEMS[PendingDecision / DebtState / AuctionState / TradeState]
    end

    subgraph LegacyBridge[Legacy Runtime Bridge]
        SESSIONBRIDGE[GameSessionBridgeFactory]
        PROJECTOR[LegacySessionProjector]
        RESTORER[LegacySessionRuntimeRestorer]
    end

    GAME --> HOSTFACTORY
    HOST --> GAME
    LOCALACTIONS --> GAME
    HOSTFACTORY --> CLIENTAPP
    HOSTFACTORY --> ASSEMBLY
    HOSTFACTORY --> SHELL
    HOSTFACTORY --> UI
    HOSTFACTORY --> RUNTIMEFACTORY
    HOSTFACTORY --> VIEW
    HOSTFACTORY --> SESSIONPRESENT
    SHELL --> TURN
    SHELL --> BOT
    SHELL --> QUERIES
    SHELL --> REATTACH
    UI --> TURN
    UI --> BOT
    UI --> VIEW
    RUNTIMEFACTORY --> BOOTSTRAP
    RUNTIMEFACTORY --> DEBUG
    BOOTSTRAP --> RESTORER
    RESTORER --> LEGACY
    CLIENTAPP --> HOST
    CLIENTAPP --> RUNTIMEFACTORY
    CLIENTAPP --> UI
    SESSIONBRIDGE --> APPFACTORY
    SESSIONBRIDGE --> AUCTION
    SESSIONBRIDGE --> TRADE
    SESSIONBRIDGE --> DEBT
    SESSIONBRIDGE --> APP
    ASSEMBLY --> SESSIONBRIDGE
    ASSEMBLY --> RUNTIMEFACTORY
    ASSEMBLY --> UI
    LOCALPERSIST --> HOST
    GAME --> LOCALPERSIST
    APP --> PURCHASE
    APP --> DEBT
    APP --> AUCTION
    APP --> TRADE
    APP --> PERSIST
    LOCALPERSIST --> PERSIST
    PURCHASE --> SESSION
    DEBT --> SESSION
    AUCTION --> SESSION
    TRADE --> SESSION
    APP --> TURN
    APP --> SUBSYSTEMS
    PERSIST --> STORE
    INFRA --> STORE
    STORE --> JSON
    APPFACTORY --> APP
    APPFACTORY --> PROJECTOR
    REATTACH --> APP
    PROJECTOR --> APP
    RESTORER --> LEGACY
    LEGACY --> PROJECTOR
    SESSION --> RESTORER
```

What is important here:

- `Game` is now more clearly a desktop host/compatibility surface, not the only place where wiring lives
- desktop code is explicitly split into `client.desktop`, `assembly`, `shell`, `runtime`, `session`, and `ui`
- turn flow, bot flow, and desktop session state are now separated into `turn`, `bot`, and `session` packages
- local save/load still works through a narrow `SessionHost` seam instead of direct rebuild callbacks
- authoritative state still lives in `SessionState` and related domain records, not only in live UI/runtime objects
- the Processing entry app and runtime now both live under `client.desktop`, so the root package no longer owns desktop bootstrap/runtime classes
- desktop-only global flags such as debug mode and skip-animations now live in explicit `client.desktop` settings/state helpers instead of `MonopolyApp`
- desktop control-layer and font resources are also isolated behind `client.desktop` runtime resource helpers instead of hanging off `MonopolyApp`
- the current Processing app instance is now accessed through an explicit `client.desktop` context seam instead of `MonopolyApp.self`
- shared rendering helpers now depend on a small `client.desktop` rendering context seam instead of the full `MonopolyApp` type
- frame/layout/session orchestration code now reads time and viewport state through `MonopolyRuntime` helpers instead of reaching directly into Processing app fields
- projected desktop session views now read popup state through explicit collaborators instead of depending on the full runtime shell
- the remaining `Game` host constructor is now driven through an explicit desktop bootstrap factory instead of assembling controls, shell, and presentation inline
- embedded desktop session hosting now targets a small hosted-game interface instead of depending on the full `Game` host type for normal session/view lifecycle operations
- the legacy bridge is still present because the Processing desktop client still runs on legacy runtime objects
- the main remaining monolith is the `Game` host itself, which now delegates more but still exposes many compatibility hooks for tests and the current desktop client

## 3. Current Practical Runtime Shape

This is the short “how the running app is assembled today” view.

```mermaid
flowchart LR
    GAME[Game]
    HOSTFACTORY[GameDesktopHostFactory]
    SHELL[GameDesktopShellCoordinator]
    ASSEMBLY[GameDesktopAssemblyFactory]
    PRESENTATION[GameDesktopPresentationHost]
    SESSIONBRIDGE[GameSessionBridgeFactory]
    RUNTIME[GameRuntimeAssemblyFactory]
    FRAME[GameFrameCoordinator]
    APP[SessionApplicationService]
    STATE[SessionState]
    LEGACY[Legacy runtime objects]

    GAME --> HOSTFACTORY
    HOSTFACTORY --> SHELL
    HOSTFACTORY --> ASSEMBLY
    GAME --> PRESENTATION
    PRESENTATION --> SHELL
    PRESENTATION --> FRAME
    ASSEMBLY --> SESSIONBRIDGE
    ASSEMBLY --> RUNTIME
    HOSTFACTORY --> FRAME
    SESSIONBRIDGE --> APP
    SHELL --> APP
    APP --> STATE
    RUNTIME --> LEGACY
    APP --> LEGACY
    FRAME --> GAME
```

This is already much closer to backend-safe behavior than the original project shape, but not fully backend-clean yet because:

- the desktop client still owns the authoritative application service instance locally
- the bridge still mutates legacy runtime objects in-process
- `Game` still exposes a broad compatibility surface for tests and local desktop orchestration, even though frame and session-view work now sits behind `GameDesktopPresentationHost`

## 4. Current Package View

This is the shortest package-oriented map of the current gameplay presentation structure.

```mermaid
flowchart TD
    ROOT[presentation.game]
    CLIENTROOT[client]
    ROOT --> BOT[bot]
    ROOT --> SESSION[session]
    ROOT --> TURN[turn]
    ROOT --> DESKTOP[desktop]
    CLIENTROOT --> CLIENTDESKTOP[desktop]

    DESKTOP --> ASSEMBLY[assembly]
    DESKTOP --> SHELL[shell]
    DESKTOP --> RUNTIME[runtime]
    DESKTOP --> DESKTOPSESSION[session]
    DESKTOP --> UI[ui]
```

Useful mental model:

- `bot`: bot scheduling and bot turn stepping
- `session`: desktop session state/presentation coordination
- `turn`: actual turn flow orchestration
- `client.desktop`: Processing app-facing shell and runtime adapters around the embedded local host
- `desktop.assembly`: object graph construction
- `desktop.shell`: orchestration between host and extracted coordinators
- `desktop.runtime`: legacy runtime bootstrap and lifecycle
- `desktop.session`: session bridge and restored-session reattachment
- `desktop.ui`: controls, layout, frame rendering, input binding, and the extracted desktop presentation host

## 5. Target Backend-Ready Architecture

This is the intended future shape after the remaining local cleanup and server extraction.

```mermaid
flowchart LR
    subgraph Client[Processing Client]
        CLIENTUI[Board / sidebar / popup rendering]
        CLIENTVM[View state mapping]
        CLIENTNET[Command + snapshot transport]
    end

    subgraph Server[Backend Session Host]
        API[HTTP / WebSocket]
        HOST[Session host / command queue]
        APP[Application services]
        BOTS[Bot orchestrator]
        SAVE[Persistence service]
    end

    subgraph Core[Shared Core]
        DOMAIN[Domain rules + SessionState + commands + events]
    end

    subgraph Storage[Persistence]
        SNAPSHOT[Snapshot store / database]
    end

    CLIENTUI --> CLIENTVM
    CLIENTVM --> CLIENTNET
    CLIENTNET --> API
    API --> HOST
    HOST --> APP
    APP --> DOMAIN
    BOTS --> APP
    SAVE --> DOMAIN
    SAVE --> SNAPSHOT
    APP --> SAVE
    APP --> API
```

What changes at that point:

- the server owns authoritative state
- bots run on the server
- the client only sends commands and renders approved state
- save/load and reconnect semantics are the same system, not separate systems

## 6. Migration Summary

```mermaid
flowchart TD
    A[Legacy single-process game] --> B[Local separation]
    B --> C[Persistence-ready authoritative session]
    C --> D[Authoritative continuation / callback elimination]
    D --> E[Local load reattachment]
    E --> F[Backend session host]
```

Current practical status:

- `A -> B` is largely done
- `C`, `D`, and `E` are now substantially in place
- the remaining local cleanup now mostly means shrinking `Game` further and reducing remaining desktop-host compatibility glue
- `F` is still the next major architecture milestone after that
