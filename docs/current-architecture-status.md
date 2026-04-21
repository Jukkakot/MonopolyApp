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
  - Processing app-facing shell/runtime adapters, runtime resources, explicit client-global desktop settings, and client context around the embedded local client session
- `host.session.local`
  - embedded host-owned session lifecycle, persistence, snapshot publication, hosted-game lifecycle, and test-access seams for the in-process desktop mode
  - embedded host-owned local game loop coordination now also drives bot stepping outside the presentation frame coordinator
- `presentation.game.desktop.session`
  - session bridge and restored-session reattachment adapters around the embedded host
- `client.session.desktop`
  - desktop client-session shell/runtime glue, client-owned local session control callbacks such as save/load triggers, and the app-facing desktop session runtime port
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

### Important current bridge classes

These are still transitional and should be treated as controlled legacy seams:

- `fi.monopoly.components.Game`
- `fi.monopoly.client.desktop.MonopolyApp`
- `fi.monopoly.client.desktop.MonopolyRuntime`
- `fi.monopoly.presentation.game.desktop.shell.GameDesktopShellCoordinator`
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

The current shell is much cleaner than before, but some interfaces still bundle:

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

### Blocker A: no explicit client session interface yet

The Processing client should depend on a narrow session-facing interface such as:

- submit command
- receive snapshot/view updates
- query current connection/session status

That interface should work for both:

- local embedded host
- remote backend host

### Blocker B: bot ownership is only partially host-owned so far

Bot coordination is cleaner now, and embedded local mode already routes bot stepping through a host-owned loop.
But the concrete bot collaborators still mostly live under presentation-era packages instead of a clearer host-side package root.

That said, one important narrowing step is now in place:

- `host.bot` no longer reaches directly into desktop runtime popup services, trade controllers, or turn-player-only projected view suppliers
- those dependencies now cross the boundary through a dedicated desktop interaction adapter

Backend-ready target:

- bot loop belongs to the host
- client only renders resulting snapshots

### Blocker C: legacy bridge still sits between app and runtime in the same process

This is the biggest structural bridge still left.

We need one more narrowing step so that:

- session host output becomes client-facing session state/view state
- legacy runtime reconstruction becomes a client adapter

### Blocker D: no real server package root yet

There is still no concrete:

- `server.session`
- `server.transport`
- `client.session`

That means the architecture is backend-friendly in shape, but not yet backend-ready in project structure.

## Recommended Immediate Architectural Focus

If the goal is fastest progress toward backend-ready architecture, the next work should prioritize:

1. introducing a client-facing session host interface
2. moving bot ownership behind that host interface
3. separating client-local runtime reconstruction from authoritative session execution
4. only after that, building embedded host and remote host implementations

That path is faster than continuing endless local `Game` cleanup in isolation.

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
