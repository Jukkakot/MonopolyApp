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
        HOSTEDADAPTER[GameBackedDesktopHostedGame]
        HOSTFACTORY[GameDesktopHostFactory]
        EMBHOST[EmbeddedDesktopSessionHost]
        HOST[host.session.local.DesktopSessionHostCoordinator]
    end

    subgraph DesktopPresentation[Desktop Presentation]
        CLIENTAPP[client.desktop]
        CLIENTSESSION[client.session.desktop]
        CLIENTRUNTIME[DesktopClientSessionRuntime]
        ASSEMBLY[desktop.assembly]
        SHELLSESSION[GameDesktopSessionCoordinator]
        SHELLPRESENT[GameDesktopPresentationCoordinator]
        UI[desktop.ui]
        UICONTROLS[GameUiSessionControls]
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
        BOTPORT[HostBotInteractionAdapter]
        SESSIONPRESENT[session.GameSessionStateCoordinator]
        QUERIES[session.GameSessionQueries]
    end

    subgraph Application[Application Layer]
        APP[SessionApplicationService]
        CMDPORT[client.session.SessionCommandPort]
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
    HOST --> HOSTEDADAPTER
    HOSTEDADAPTER --> GAME
    HOSTFACTORY --> CLIENTAPP
    HOSTFACTORY --> CLIENTSESSION
    CLIENTAPP --> CLIENTRUNTIME
    CLIENTSESSION --> CLIENTRUNTIME
    HOSTFACTORY --> ASSEMBLY
    SHELLPRESENT --> UICONTROLS
    ASSEMBLY --> UICONTROLS
    HOSTFACTORY --> SHELLSESSION
    HOSTFACTORY --> SHELLPRESENT
    HOSTFACTORY --> UI
    HOSTFACTORY --> RUNTIMEFACTORY
    HOSTFACTORY --> VIEW
    HOSTFACTORY --> SESSIONPRESENT
    SHELLPRESENT --> TURN
    SHELLPRESENT --> BOT
    ASSEMBLY --> BOTPORT
    BOT --> BOTPORT
    SHELLPRESENT --> QUERIES
    SHELLSESSION --> REATTACH
    UI --> TURN
    UI --> BOT
    UI --> VIEW
    RUNTIMEFACTORY --> BOOTSTRAP
    RUNTIMEFACTORY --> DEBUG
    BOOTSTRAP --> RESTORER
    RESTORER --> LEGACY
    CLIENTAPP --> EMBHOST
    EMBHOST --> HOST
    EMBHOST --> HOSTEDADAPTER
    EMBHOST -.->|implements| CMDPORT
    APP -.->|implements| CMDPORT
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
- client-owned save/load trigger callbacks now also live under `client.session.desktop` instead of the presentation-session package
- the Processing app shell now talks to a single `DesktopClientSessionRuntime` port instead of directly forwarding embedded-session shell methods one by one
- the old generic `ClientSession` type is gone; the client now depends on an explicit `ClientSessionUpdates` gateway for session-update subscriptions, while host-side restore authority stays behind `SessionHost`
- `ClientSessionUpdates` also no longer exposes a direct snapshot getter; embedded local mode now publishes snapshot state through listener updates only
- embedded desktop frame advancement no longer lives on the client session-update gateway; the Processing runtime now gets that local-only behavior through a separate desktop frame-driver seam
- `ClientSessionUpdates` is now closer to a pure client state/view subscription seam, while fresh-session and local persistence workflows go through a separate desktop-local controls port
- embedded live render access also no longer lives on the session-update gateway; desktop mode reaches it through a dedicated local view port
- the live render view type itself no longer sits in the transport-neutral client-session package; it now lives under the desktop-local session package
- the desktop app shell now owns a small `DesktopClientSessionModel` fed by `ClientSessionUpdates` listener updates, so raw snapshot/listener pass-through no longer lives on `DesktopClientSessionRuntime`
- the desktop app shell now also owns a small `DesktopClientRenderModel`, so `DesktopClientSessionRuntime` no longer exposes raw `currentView()` polling either
- `DesktopAppShell` now also receives one bundled client-host binding and narrow `DesktopRuntimeAccess` port instead of constructing the embedded local host shell/bootstrap graph itself
- pause, bot-speed, language, and local save/load UI actions now also cross into desktop presentation through a dedicated `GameUiSessionControls` port instead of the broad `GamePresentationFactory.Hooks` surface
- desktop control-layer and font resources are also isolated behind `client.desktop` runtime resource helpers instead of hanging off `MonopolyApp`
- shared rendering helpers now depend on a small `client.desktop` rendering context seam instead of the full `MonopolyApp` type
- frame/layout/session orchestration code now reads time and viewport state through `MonopolyRuntime` helpers instead of reaching directly into Processing app fields
- desktop app-shell runtime/persistence adapters now receive the active runtime instance explicitly from `DesktopRuntimeBridge` instead of calling the global runtime singleton directly
- embedded-local bootstrapping now also lives behind an explicit `EmbeddedLocalDesktopClientBindingFactory`, which is closer to the future point where embedded and remote client bindings can be swapped
- the Processing input observer now also pulls its event bus through `DesktopAppShell` instead of reaching for a global runtime singleton
- the Processing app constructor no longer creates a placeholder runtime singleton before real desktop bootstrap happens
- shared utility helpers no longer rely on a global current-app context at all; rendering callers pass an explicit rendering context
- property-level runtime checks for building supply and utility rent now also resolve through the owning player's runtime/session context instead of the global runtime singleton
- projected desktop session views now read popup state through explicit collaborators instead of depending on the full runtime shell
- the remaining `Game` host constructor is now driven through an explicit desktop bootstrap factory instead of assembling controls, shell, and presentation inline
- embedded desktop session hosting now targets a small hosted-game interface instead of depending on the full `Game` host type for normal session/view lifecycle operations
- app-shell and embedded-client test-only hosted-game access is now isolated behind an explicit `testAccess()` seam instead of living on the normal production shell API
- concrete local hosted-game creation now also lives behind an assembly-side factory seam, so the client-desktop runtime bridge no longer constructs `Game` directly
- embedded local session lifecycle, persistence, and snapshot publication now live on the host side, and the embedded host now exposes the client-session seam directly instead of routing through `LocalDesktopClientSession`
- the embedded host now also reaches the concrete game through a dedicated `DesktopHostedGame` adapter instead of using `Game` itself as the hosted-session contract
- integration and smoke tests now increasingly inspect `Game` through one explicit test facade instead of reaching into scattered private host internals
- the local hosted-game lifecycle/view/test-access seams now also live under `host.session.local` instead of the presentation package tree
- the hosted-game seam is now split between host-owned frame advancement and a narrower client-facing render view, so bot/session ticking no longer shares the same interface surface as drawing
- embedded local mode now also runs bot stepping from an explicit host-owned game loop coordinator instead of from the presentation frame coordinator itself
- desktop-local popup, trade, and projected-view dependencies now cross into `host.bot` through a single `HostBotInteractionAdapter` seam
- bot turn contexts now build projected views for the actual acting player, which is important when debt resolution is driven by someone other than the nominal current turn owner
- `SessionCommandPort` in `client.session` is now the transport-neutral command submission and state query seam: `handle(SessionCommand)` + `currentState()` — any future remote host transport adapter only needs to implement these two methods
- `SessionApplicationService` now implements `SessionCommandPort`, so the embedded local mode works without behavioral change while the seam is ready for a remote backend
- `EmbeddedDesktopSessionHost` now also implements `SessionCommandPort` and acts as the single named command entry point on the host side, so command submission and snapshot publication both flow through it
- `HostedLocalSession` extends `SessionCommandPort`, making it a requirement for all future local host implementations
- the five presentation-layer session adapters (debt, auction, purchase, trade) now depend only on `SessionCommandPort` instead of the full `SessionApplicationService` — any future transport adapter automatically satisfies their constructor requirement
- `BotTurnScheduler` no longer imports `DesktopClientSettings` directly; the `skipAnimations` flag is now injected as a `BooleanSupplier`, removing the desktop-specific global dependency from the `host.bot` layer
- `GameSessionStateCoordinator.onDebtStateChanged()` no longer takes `SessionApplicationService`; the `clearDebtOverride` behavior is now passed as a `Runnable` callback, removing the application-service dependency from the presentation-session coordinator
- the legacy bridge is still present because the Processing desktop client still runs on legacy runtime objects
- the main remaining monolith is the `Game` host itself, which now delegates more but still exposes many compatibility hooks for tests and the current desktop client

## 3. Current Practical Runtime Shape

This is the short “how the running app is assembled today” view.

```mermaid
flowchart LR
    GAME[Game]
    HOSTEDADAPTER[GameBackedDesktopHostedGame]
    HOSTFACTORY[GameDesktopHostFactory]
    EMBHOST[EmbeddedDesktopSessionHost]
    SHELLSESSION[GameDesktopSessionCoordinator]
    SHELLPRESENT[GameDesktopPresentationCoordinator]
    ASSEMBLY[GameDesktopAssemblyFactory]
    PRESENTATION[GameDesktopPresentationHost]
    SESSIONBRIDGE[GameSessionBridgeFactory]
    RUNTIME[GameRuntimeAssemblyFactory]
    FRAME[GameFrameCoordinator]
    APP[SessionApplicationService]
    CMDPORT[SessionCommandPort]
    STATE[SessionState]
    LEGACY[Legacy runtime objects]

    GAME --> HOSTFACTORY
    HOSTFACTORY --> EMBHOST
    EMBHOST --> HOSTEDADAPTER
    HOSTEDADAPTER --> GAME
    EMBHOST -.->|implements| CMDPORT
    APP -.->|implements| CMDPORT
    HOSTFACTORY --> SHELLSESSION
    HOSTFACTORY --> SHELLPRESENT
    HOSTFACTORY --> ASSEMBLY
    GAME --> PRESENTATION
    PRESENTATION --> SHELLPRESENT
    PRESENTATION --> FRAME
    ASSEMBLY --> SESSIONBRIDGE
    ASSEMBLY --> RUNTIME
    HOSTFACTORY --> FRAME
    SESSIONBRIDGE --> CMDPORT
    SHELLSESSION --> APP
    APP --> STATE
    RUNTIME --> LEGACY
    APP --> LEGACY
    FRAME --> GAME
```

This is already much closer to backend-safe behavior than the original project shape, but not fully backend-clean yet because:

- the desktop client still owns the authoritative application service instance locally
- the bridge still mutates legacy runtime objects in-process
- the desktop client now has its own listener-fed session and render models, but the rendered live view is still host-provided rather than a transport-neutral client projection
- `Game` still exposes a broad compatibility surface for tests and local desktop orchestration, even though frame and session-view work now sits behind `GameDesktopPresentationHost`

## 4. Current Package View

This is the shortest package-oriented map of the current gameplay presentation structure.

```mermaid
flowchart TD
    ROOT[presentation.game]
    CLIENTROOT[client]
    HOSTROOT[host.session.local]
    ROOT --> BOT[bot]
    ROOT --> SESSION[session]
    ROOT --> TURN[turn]
    ROOT --> DESKTOP[desktop]
    CLIENTROOT --> CLIENTDESKTOP[desktop]
    CLIENTROOT --> CLIENTSESSION[session]

    DESKTOP --> ASSEMBLY[assembly]
    DESKTOP --> SHELL[shell]
    DESKTOP --> RUNTIME[runtime]
    DESKTOP --> DESKTOPSESSION[session]
    DESKTOP --> UI[ui]

    CLIENTSESSION --> CMDPORT[SessionCommandPort]
    CLIENTSESSION --> CLIENTUPDATES[ClientSessionUpdates]
    CLIENTSESSION --> SNAPSHOT[ClientSessionSnapshot]

    HOSTROOT --> EMBHOST[EmbeddedDesktopSessionHost]
    HOSTROOT --> HOSTEDLOCAL[HostedLocalSession]
```

Useful mental model:

- `bot`: bot scheduling and bot turn stepping
- `session`: desktop session state/presentation coordination
- `turn`: actual turn flow orchestration
- `client.desktop`: Processing app-facing shell and runtime adapters around the embedded local host
- `client.session`: transport-neutral seam types — `SessionCommandPort` (send commands), `ClientSessionUpdates` (receive snapshots), `ClientSessionSnapshot` (snapshot payload)
- `desktop.assembly`: object graph construction
- `desktop.shell`: orchestration between host and extracted coordinators
- `desktop.runtime`: legacy runtime bootstrap and lifecycle
- `desktop.session`: session bridge and restored-session reattachment
- `desktop.ui`: controls, layout, frame rendering, input binding, and the extracted desktop presentation host
- `host.session.local`: `EmbeddedDesktopSessionHost` (single command entry point + snapshot publisher) and `HostedLocalSession` (combines all local host seams)

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
- the client-facing backend contract is now fully defined: `SessionCommandPort` (commands in), `ClientSessionUpdates` (snapshots out), `ClientSessionSnapshot` (transport-neutral payload carrying the full `SessionState`) — all in `client.session`
- `ClientSessionSnapshot` now carries the complete authoritative `SessionState`, making snapshots self-sufficient: a client can reconstruct its presentation model from the snapshot without a separate state query to the host
- `EmbeddedDesktopSessionHost` now implements `SessionCommandPort` and is the single named command entry point for the embedded local host — a remote transport adapter needs only to implement the same two-method interface
- the five presentation-layer adapters (debt, auction, purchase, trade) already depend only on `SessionCommandPort`, so they are ready to be rewired to either an embedded or remote host without behavioral change
- the remaining local cleanup means: (1) moving adapter assembly out of `Game` and into host-level wiring so adapters receive `EmbeddedDesktopSessionHost` directly, (2) separating legacy runtime reconstruction from authoritative session execution
- `F` is the next major architecture milestone, with the main prerequisite being a concrete server-side session host that implements the already-defined `SessionCommandPort` + `ClientSessionUpdates` contract
