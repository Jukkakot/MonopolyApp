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

## 2. Current Direction In This Repo

This is the rough shape now after the local separation work already implemented.

```mermaid
flowchart LR
    subgraph Presentation[Presentation Layer]
        GAME[Game]
        POPUPS[Popup adapters / UI renderers]
        VIEW[Session view helpers]
        LOCALINPUT[Keyboard / buttons / local player input]
    end

    subgraph Application[Application Layer]
        APP[SessionApplicationService]
        HOST[SessionHost]
        PURCHASE[Property purchase handlers]
        DEBT[Debt handlers]
        AUCTION[Auction handlers]
        TRADE[Trade handlers]
        PERSIST[SessionPersistenceService]
        LOCALPERSIST[LocalSessionPersistenceCoordinator]
    end

    subgraph PersistenceInfra[Persistence Infrastructure]
        STORE[SessionSnapshotStore]
        JSON[JsonFileSessionSnapshotStore]
    end

    subgraph Domain[Authoritative State]
        SESSION[SessionState]
        TURN[TurnState / TurnContinuationState]
        SUBSYSTEMS[PendingDecision / DebtState / AuctionState / TradeState]
    end

    subgraph LegacyBridge[Legacy Runtime Bridge]
        FACTORY[LegacySessionApplicationFactory]
        PROJECTOR[LegacySessionProjector]
        RESTORER[LegacySessionRuntimeRestorer]
        LEGACY[Players / Property objects / board runtime]
    end

    LOCALINPUT --> GAME
    GAME --> POPUPS
    GAME --> VIEW
    GAME --> APP
    GAME --> LOCALPERSIST
    LOCALPERSIST --> HOST
    POPUPS --> APP
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
    STORE --> JSON
    FACTORY --> APP
    FACTORY --> PROJECTOR
    PROJECTOR --> APP
    RESTORER --> LEGACY
    LEGACY --> PROJECTOR
    SESSION --> RESTORER
```

What is important here:

- authority has moved much more clearly into command/state/application types
- `Game` is no longer the only place that understands gameplay progression
- persistence now works against `SessionState`, not only live UI/runtime state
- local save/load now depends on a small `SessionHost` seam instead of directly owning rebuild/state callbacks
- snapshot storage has a swap-ready store seam, so local JSON is no longer the only assumed persistence backend
- the legacy `SessionApplicationService` wiring has been centralized behind `LegacySessionApplicationFactory`, which makes the remaining runtime bridge more explicit
- there is still a legacy bridge because the Processing client still runs against runtime objects

## 3. Current Practical Runtime Shape

This is the short “how the running app behaves today” view.

```mermaid
sequenceDiagram
    participant User as Human or Bot
    participant UI as Game / Popup UI
    participant App as SessionApplicationService
    participant State as SessionState
    participant Legacy as Legacy runtime objects
    User ->> UI: input / decision
    UI ->> App: command
    App ->> State: validate + mutate authoritative state
    App ->> Legacy: call bridge where legacy runtime mutation is still needed
    App -->> UI: current state / result
    UI -->> User: render updated state
```

This is already much closer to backend-safe behavior than the original project shape, but not fully backend-clean yet.

## 4. Target Backend-Ready Architecture

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

## 5. Migration Summary

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
- `F` is the next major architecture milestone, but there is still some local cleanup value before starting it fully
