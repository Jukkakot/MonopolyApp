# Separation Program Index

## Purpose

This document is the entry point for the full local-separation-to-server roadmap.

It collects the current design documents into one implementation-oriented program:

- what exists
- what each document is for
- in which order to implement the work
- which PRs are currently specified well enough to build

The goal is that implementation can later proceed in controlled slices without reopening architecture from scratch every
time.

## Program Goal

Move the current Monopoly Processing app toward this target:

- UI/presentation separated from game-rule authority
- command-driven authoritative state
- bots and humans using the same decision boundary
- later server extraction possible without rewriting rules again

The migration is explicitly local-first, server-later.

## Document Set

### 1. Architecture and strategy

- [architecture-separation-and-server-plan.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/architecture-separation-and-server-plan.md)

Purpose:

- explains the high-level architecture choice
- defines the 4-layer target:
    - domain
    - application
    - presentation
    - infrastructure
- explains why server should not be built first

### 2. Core state/command/event spec

- [session-state-command-spec.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/session-state-command-spec.md)

Purpose:

- defines the future authoritative model:
    - `SessionState`
    - `SeatState`
    - `TurnState`
    - `PendingDecision`
    - `DebtState`
    - `AuctionState`
    - `TradeState`
    - commands
    - domain events

Use this as the conceptual source of truth.

### 3. First-slice command matrix

- [command-matrix-first-slices.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/command-matrix-first-slices.md)

Purpose:

- turns the spec into command-by-command behavior rules
- defines actor/phase/mutation/event expectations
- covers:
    - property purchase
    - rent
    - debt opening/remediation shape
    - auction
    - autoplay control

Use this when designing validators and command handlers.

### 4. Migration map from current codebase

- [migration-map-local-separation.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/migration-map-local-separation.md)

Purpose:

- maps current classes to future architectural roles
- identifies transitional adapters
- defines what survives vs what becomes a bridge only

Use this when deciding where code should move and what not to grow anymore.

### 5. PR1 design note

- [pr1-design-note-session-state-seam.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr1-design-note-session-state-seam.md)

Purpose:

- defines the first seam-creation PR
- minimal new types
- minimal field set
- exact in-scope / out-of-scope rules

Status:

- ready for implementation planning

### 6. PR2 design note

- [pr2-design-note-property-purchase-slice.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr2-design-note-property-purchase-slice.md)

Purpose:

- defines the first real behavior migration:
    - property purchase
    - explicit buy/decline commands
    - authoritative pending decision
    - authoritative auction start on decline

Status:

- ready for implementation planning

### 7. PR3 design note

- [pr3-design-note-rent-and-debt-opening.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr3-design-note-rent-and-debt-opening.md)

Purpose:

- defines the second real behavior migration:
    - rent payment opening
    - immediate payment vs debt opening
    - authoritative debt state start

Status:

- ready for implementation planning

### 8. PR4 design note

- [pr4-design-note-debt-remediation.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr4-design-note-debt-remediation.md)

Purpose:

- defines debt remediation migration:
    - mortgage
    - building sale
    - explicit `PayDebtCommand`
    - bankruptcy declaration

Status:

- ready for implementation planning

### 9. PR5 design note

- [pr5-design-note-auction-flow.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr5-design-note-auction-flow.md)

Purpose:

- defines full auction migration:
    - bid/pass commands
    - authoritative auction turn order
    - authoritative leader/highest bid
    - explicit auction terminal resolution

Status:

- ready for implementation planning

### 10. PR6 design note

- [pr6-design-note-trade-flow.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr6-design-note-trade-flow.md)

Purpose:

- defines trade migration:
    - authoritative `TradeState`
    - edit/submit/accept/decline/counter/cancel commands
    - preserving current visual trade editor as presentation

Status:

- ready for implementation planning

### 11. PR7 design note

- [pr7-design-note-bot-command-unification.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr7-design-note-bot-command-unification.md)

Purpose:

- defines bot/autoplay execution unification around the command boundary

Status:

- ready for implementation planning

### 12. PR8 design note

- [pr8-design-note-game-presentation-shell-cleanup.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr8-design-note-game-presentation-shell-cleanup.md)

Purpose:

- defines aggressive `Game` god-object cleanup into a presentation shell

Status:

- ready for implementation planning

## Decisions Already Locked

These should now be treated as baseline assumptions unless we explicitly reopen them.

### Product / control decisions

- language is client-local
- human can play against bots normally
- a human-owned seat can be temporarily delegated to autoplay
- autoplay may be interrupted immediately
- autoplay may also answer pending decisions immediately without grace delay
- one client controlling multiple seats should remain architecturally possible, but MVP UX should not optimize for it
  yet

### Networking / state decisions

- full snapshots first
- diffs later
- Processing remains the first/main client for now
- future multiple client types should remain possible

### Architecture decisions

- server is not the first step
- separation starts locally
- authority should be on `seat` / `SessionState`, not client UI
- `recent messages` are projection from events, not authoritative state
- debt requires explicit `PayDebtCommand`, not auto-pay
- auctions should have an explicit terminal resolution state
- property purchase should use explicit commands, not only a generic decision command
- trade editing starts with one generic edit command

## Implementation Program Order

This is the recommended implementation order.

### Phase 1: Create the seam

PR1:

- session snapshot model
- command/result/event primitives
- application service shell
- projector from legacy runtime to new state

Primary document:

- [pr1-design-note-session-state-seam.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr1-design-note-session-state-seam.md)

### Phase 2: Migrate property purchase

PR2:

- pending decision for property purchase
- explicit buy/decline commands
- decline creates authoritative auction state

Primary document:

- [pr2-design-note-property-purchase-slice.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr2-design-note-property-purchase-slice.md)

### Phase 3: Migrate rent and debt opening

PR3:

- immediate payment vs debt opening
- authoritative debt state start

Primary document:

- [pr3-design-note-rent-and-debt-opening.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr3-design-note-rent-and-debt-opening.md)

### Phase 4: Migrate debt remediation

PR4:

- mortgage command path
- building sale command path
- explicit debt payment
- bankruptcy command path
- `DebtController` reduced toward helper/adapter status

Primary document:

- [pr4-design-note-debt-remediation.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr4-design-note-debt-remediation.md)

### Phase 5: Migrate auction flow

PR5:

- full `AuctionState`
- bid/pass commands
- terminal auction resolution handling

Primary document:

- [pr5-design-note-auction-flow.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr5-design-note-auction-flow.md)

### Phase 6: Migrate trade flow

PR6:

- authoritative `TradeState`
- generic edit command
- submit/accept/decline/counter/cancel flow
- UI becomes trade-state renderer/editor instead of trade authority

Primary document:

- [pr6-design-note-trade-flow.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr6-design-note-trade-flow.md)

### Phase 7: Unify bots behind commands

PR7:

- bots stop resolving through popup/UI pathways
- bots use same command boundary as humans across slices

Primary document:

- [pr7-design-note-bot-command-unification.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr7-design-note-bot-command-unification.md)

### Phase 8: Shrink `Game` to presentation shell

PR8:

- render from view state
- send commands only
- no rule ownership

Primary document:

- [pr8-design-note-game-presentation-shell-cleanup.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr8-design-note-game-presentation-shell-cleanup.md)

### Phase 9: Persistence-ready authoritative session

PR9:

- stable session snapshot boundary
- save/load on authoritative session
- restore across pending decision / debt / auction / trade

Primary document:

- [pr9-design-note-persistence-ready-session.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr9-design-note-persistence-ready-session.md)

### Phase 10: Server extraction

PR10:

- backend session host
- command transport
- full snapshot sync
- backend bots/autoplay

Primary document:

- [pr10-design-note-server-extraction-mvp.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr10-design-note-server-extraction-mvp.md)

### Phase 11: Callback elimination and authoritative turn continuation

PR11:

- replace callback-owned gameplay continuation with authoritative continuation state
- make save/load truly resumable across property purchase / debt / auction

Primary document:

- [pr11-design-note-turn-continuation-state.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr11-design-note-turn-continuation-state.md)

### Phase 12: Local load reattachment

PR12:

- reattach local runtime/presentation to restored authoritative session
- make saved local sessions actually resumable in the client

Primary document:

- [pr12-design-note-local-load-reattachment.md](/E:/Documents/ProcessingProjects/MonopolyApp/docs/pr12-design-note-local-load-reattachment.md)

## Which PRs Are Ready Enough To Implement

### Ready now

- PR1
- PR2
- PR3
- PR4
- PR5
- PR6
- PR7
- PR8
- PR9
- PR10
- PR11
- PR12

These are specified well enough that implementation could start without reopening architecture first.

### Current branch status

On branch `separation-program`:

- PR1 through PR8 are already substantially implemented
- the next planned implementation wave is PR9
- PR10 remains design-ready but should follow PR9
- PR11 and PR12 now define the remaining local-authority cleanup before backend work

## Suggested Task Breakdown

This section is the practical backlog skeleton.

### Task Group A: Foundation

1. Create new `domain/session`, `domain/turn`, `domain/decision`, `application/*`, and `presentation/session` packages
2. Add PR1 types
3. Add `LegacySessionProjector`
4. Add `SessionApplicationService`
5. Add projector/service tests

### Task Group B: Property purchase

1. Add property purchase commands
2. Extend `PendingDecision` with typed property purchase payload
3. Add initial `AuctionState`
4. Add property purchase command handler
5. Add popup adapter for property purchase decision
6. Remove popup-owned property purchase mutation
7. Add command/integration tests

### Task Group C: Rent and debt opening

1. Add `DebtStateModel` and related types
2. Add `PayDebtCommand`
3. Add rent/debt opening handler
4. Add legacy payment gateway
5. Make debt UI render from authoritative debt-open state
6. Remove popup-owned debt opening branch
7. Add command/integration tests

### Task Group D: Debt remediation

1. Add debt remediation commands
2. Add debt remediation command handler
3. Add legacy debt remediation gateway
4. Route debt UI actions to explicit commands
5. Make bots use the same debt action path
6. Reduce `DebtController` to helper/adapter role

### Task Group E: Auction

1. Add auction commands
2. Add auction command handler
3. Add legacy auction gateway
4. Route auction popup to render authoritative state
5. Make humans and bots bid/pass through commands
6. Move winner resolution to explicit terminal state handling

### Task Group F: Trade

1. Add authoritative `TradeState` types
2. Add trade commands and patch model
3. Add trade command handler
4. Add legacy trade gateway
5. Route current trade popup/editor to render authoritative trade state
6. Make human and bot trade actions use command path

## Guardrails For All Implementation Work

Every implementation PR in this program should follow these rules:

1. Do not add new gameplay authority
   into [Game.java](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components/Game.java).
2. Do not add new gameplay authority
   into [PopupService.java](/E:/Documents/ProcessingProjects/MonopolyApp/src/main/java/fi/monopoly/components/popup/PopupService.java).
3. New domain/application classes must not depend on Processing, ControlP5, or `MonopolyRuntime.get()/peek()`.
4. Transitional adapters are acceptable, but they must point from legacy world into the new seam, not the reverse.
5. Each PR should migrate one vertical slice or one foundational abstraction, not multiple large flows at once.

## What Still Needs Product/Design Input Later

These are not blockers yet, but they will need explicit decisions before their dedicated PR notes are finalized.

1. Debt remediation UI shape:
    - free-form asset panel vs narrower sub-decisions
2. Auction terminal resolution UX:
    - automatic finish vs explicit acknowledgement in some cases
3. Trade migration shape:
    - how much of current visual trade editor survives vs gets rebuilt around `TradeState`
4. Bot unification scope:
    - whether to keep temporary view-based heuristics longer for some systems
5. Save/load scope:
    - how early mid-decision restore must be end-to-end testable

## Recommended Immediate Next Planning Step

The next best implementation move is:

- finish validating PR8 cleanup boundaries in code
- then begin PR9 persistence boundary implementation

Reason:

- local separation is already specified and largely executable
- the next architectural risk sits at the persistence boundary, not in more local UI cleanup
