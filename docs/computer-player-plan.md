# Computer Player Plan

## Goal

Implement computer players in layers so the project first gets a stable debug bot and then a stronger strategy bot without baking all decision logic directly into `Game`.

The first bot profile is:

- `SMOKE_TEST`: minimal, deterministic, safe for regression and debug flows

The second bot profile is:

- `STRONG`: best practical strategy bot for this codebase, based on heuristics and configurable weights

## Design Principles

- Keep execution separate from decision-making.
- Keep game-state reading separate from mutable game actions.
- Make bot behavior configurable through profiles and weights, not hardcoded branching everywhere.
- Prefer score-based decisions over long chains of one-off `if` statements.
- Keep `SMOKE_TEST` simple and deterministic even after stronger bots are added.

## Architecture

Split the bot system into three layers.

### 1. Strategy

Decides what the bot wants to do.

Examples:

- buy property or decline
- end turn
- resolve popup with accept or cancel
- sell buildings or mortgage properties in debt state
- stay in jail or get out

Suggested interface:

```java
public interface ComputerStrategy {
    ComputerDecision decideTurn(GameView view, PlayerView self);
    ComputerDecision decidePopup(GameView view, PlayerView self, PopupView popup);
    ComputerDecision decideDebt(GameView view, PlayerView self, DebtView debt);
}
```

### 2. Evaluator

Calculates how valuable, dangerous, or profitable a situation is.

Examples:

- property strategic value
- monopoly completion value
- opponent blocking value
- build return-on-investment value
- current liquidity risk
- board danger level

This keeps the `STRONG` bot explainable and tunable.

### 3. Executor

Maps a decision into current game actions.

Examples:

- roll dice
- click default popup action
- call `retryPendingDebtPayment()`
- mortgage a selected property
- sell one house
- end the turn

The executor should be shared by all bot profiles.

## Profiles

### `SMOKE_TEST`

Purpose:

- regression tests
- debug runs
- proving the game can progress without user input

Behavior:

- take the default action whenever possible
- roll dice when available
- accept default popup choices
- end turn when possible
- resolve debt with the simplest working liquidation path
- do not try to optimize buying, building, or jail decisions

This profile should stay intentionally dumb and stable.

### `STRONG`

Purpose:

- strongest practical in-project AI
- useful single-player opponent
- configurable base for future variants

Behavior:

- evaluates property purchases using strategic scoring
- protects cash reserve
- prioritizes monopoly completion
- blocks opponent monopoly completion
- builds where expected rent growth is best
- liquidates low-value assets first
- changes jail behavior based on game phase and board danger

## Strong Bot Rules

Use these rules as the base heuristics for `STRONG`.

### Cash reserve

The bot should not spend down to zero just because a property is available.

Use at least two reserve levels:

- normal reserve
- danger reserve when opponents have strong boards or the bot is financially exposed

Example:

- `minCashReserve = 250`
- `dangerCashReserve = 400`

### Property purchase priorities

Score each property instead of using a single fixed rule.

Suggested scoring inputs:

- completes own color set
- moves own color set closer to completion
- blocks opponent monopoly
- expected landing frequency
- rent growth after building
- railroad value
- utility value
- liquidity cost

Suggested decision rule:

```java
buyScore =
    colorCompletionWeight * completionValue
  + opponentBlockWeight * blockValue
  + roiWeight * roiValue
  - liquidityWeight * liquidityRisk;
```

Buy when:

- `buyScore >= buyThreshold`
- and post-purchase cash stays above the configured reserve

Allow a stronger override when the purchase completes a monopoly.

### Building priorities

Do not build evenly without evaluation.

Prioritize:

- groups with strong traffic and rent growth
- groups with cheaper houses and strong ROI
- getting to three houses before pushing to hotels

Rules:

- keep cash reserve after building
- favor 3-house pressure before hotels
- stop building when board danger is high

### Debt resolution

Debt liquidation must preserve strategic assets when possible.

Sell or mortgage in this order:

1. sell buildings from low-ROI groups first
2. mortgage isolated properties first
3. mortgage weak non-core groups next
4. preserve monopolies until forced

Avoid:

- mortgaging a street in a promising set too early
- breaking strong build groups unless there is no safer path

### Jail behavior

Use phase-aware logic.

- early game: get out quickly to buy more properties
- late game: staying in jail can be safer if the board is dangerous

Required inputs:

- number of unowned properties left
- opponent build danger
- current cash safety

## Config Model

Use profile plus config, not just enum branching.

Suggested model:

```java
public record BotConfig(
    double buyThreshold,
    int minCashReserve,
    int dangerCashReserve,
    double colorCompletionWeight,
    double opponentBlockWeight,
    double roiWeight,
    double liquidityWeight,
    boolean buyToBlockOpponent,
    boolean prioritizeThreeHouses,
    boolean preferJailLateGame
) {}
```

Suggested profile holder:

```java
public record BotProfile(
    String id,
    ComputerStrategy strategy,
    BotConfig config
) {}
```

This allows:

- one strategy implementation with multiple tuned variants
- stable regression profiles
- future UI selection per player seat

## Recommended Initial Configs

### `SMOKE_TEST`

```java
BotConfig smoke = new BotConfig(
    Double.MAX_VALUE,
    0,
    0,
    0.0,
    0.0,
    0.0,
    0.0,
    false,
    false,
    false
);
```

Interpretation:

- score thresholds effectively disable smart buying
- no strategic weighting
- use only default action paths

### `STRONG_DEFAULT`

```java
BotConfig strongDefault = new BotConfig(
    1.0,
    250,
    400,
    4.0,
    3.5,
    2.5,
    3.0,
    true,
    true,
    true
);
```

### `STRONG_AGGRESSIVE`

```java
BotConfig strongAggressive = new BotConfig(
    0.5,
    180,
    320,
    4.5,
    3.0,
    3.5,
    2.0,
    true,
    true,
    false
);
```

### `STRONG_DEFENSIVE`

```java
BotConfig strongDefensive = new BotConfig(
    1.5,
    320,
    500,
    3.5,
    4.0,
    2.0,
    4.0,
    true,
    true,
    true
);
```

These values are starting points only. They should be tuned through simulation.

## Read-Only Bot Views

Avoid giving the strategy direct access to mutable game classes.

Add read-only views such as:

- `GameView`
- `PlayerView`
- `PopupView`
- `DebtView`

These should expose only the information needed for evaluation:

- current player
- owned properties
- visible actions
- debt requirements
- build state
- board danger metrics
- unowned property count

## Decision Tracing

The stronger bot should produce reasoning strings for debugging.

Suggested shape:

```java
public record ComputerDecision(
    ComputerAction action,
    double score,
    String reason
) {}
```

Examples:

- `"Buy Orange 2: completes set and keeps reserve above 250"`
- `"Decline Utility: low ROI and cash would drop below danger reserve"`
- `"Mortgage Railroad: isolated asset with low strategic value"`

This will make tuning much easier.

## Testing Strategy

Do not rely on one smoke test only.

Required layers:

### Unit tests

- property scoring
- debt liquidation ordering
- jail decision selection
- config overrides

### Integration tests

- `SMOKE_TEST` can finish turns without user input
- `STRONG` can resolve popup and debt states
- bots do not deadlock on repeated turns

### Simulation tests

Run repeated bot-vs-bot games and collect:

- win rate
- average game length
- bankruptcy frequency
- stalled game count

This is the only realistic way to tune stronger profiles.

## Recommended Implementation Order

1. Extract current bot logic out of `Game` into dedicated strategy classes.
2. Add read-only bot view models over current game state.
3. Keep current `SMOKE_TEST` behavior working through the new strategy interface.
4. Add `BotConfig` and profile factory wiring.
5. Implement strong property-buy heuristics.
6. Implement strong debt liquidation heuristics.
7. Implement strong building heuristics.
8. Implement strong jail heuristics.
9. Add decision logging and simulation metrics.
10. Add UI selection for human/computer seat and difficulty profile.

## Explicit Non-Goals For MVP

These should not block the first stronger bot:

- trade AI
- negotiation-style behavior
- perfect probability solver
- Monte Carlo or search-heavy AI
- networked multiplayer bot hosting

Those can come later after the basic strong heuristic bot works reliably.
