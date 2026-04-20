package fi.monopoly.presentation.game.turn;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.PlayerToken;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.turn.EndTurnEffect;
import fi.monopoly.components.turn.MovePlayerEffect;
import fi.monopoly.components.turn.ShowDiceEffect;
import fi.monopoly.components.turn.ShowEndTurnEffect;
import fi.monopoly.components.turn.TurnEffect;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.components.turn.TurnPlan;
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.TurnResult;
import lombok.extern.slf4j.Slf4j;

import java.util.function.IntSupplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public final class GameTurnFlowCoordinator {
    private final MonopolyRuntime runtime;
    private final Players players;
    private final Dices dices;
    private final Board board;
    private final Animations animations;
    private final TurnEngine turnEngine;
    private final PropertyPurchaseFlow propertyPurchaseFlow;
    private final IntSupplier goMoneyAmountSupplier;
    private final Hooks hooks;
    private TurnResult prevTurnResult;

    public GameTurnFlowCoordinator(
            MonopolyRuntime runtime,
            Players players,
            Dices dices,
            Board board,
            Animations animations,
            TurnEngine turnEngine,
            PropertyPurchaseFlow propertyPurchaseFlow,
            IntSupplier goMoneyAmountSupplier,
            Hooks hooks
    ) {
        this.runtime = runtime;
        this.players = players;
        this.dices = dices;
        this.board = board;
        this.animations = animations;
        this.turnEngine = turnEngine;
        this.propertyPurchaseFlow = propertyPurchaseFlow;
        this.goMoneyAmountSupplier = goMoneyAmountSupplier;
        this.hooks = hooks;
    }

    public void rollDice() {
        hooks.updateLogTurnContext();
        if (runtime.popupService().isAnyVisible() || hooks.debtActive()) {
            log.trace("Ignoring rollDice because popupVisible={} debtStateActive={}",
                    runtime.popupService().isAnyVisible(), hooks.debtActive());
            return;
        }
        log.debug("Starting turn roll for player {}", players.getTurn().getName());
        playRound(dices.getValue());
    }

    public void endRound(boolean switchTurns) {
        hooks.updateLogTurnContext();
        if (hooks.gameOver()) {
            hooks.hidePrimaryTurnControls();
            return;
        }
        Player currentTurn = players.getTurn();
        prevTurnResult = null;
        if (switchTurns) {
            players.switchTurn();
        }
        hooks.updateLogTurnContext();
        hooks.showRollDiceControl();
        log.debug("Ending round. previousTurnPlayer={}, switchTurns={}, nextTurnPlayer={}",
                currentTurn != null ? currentTurn.getName() : "none",
                switchTurns,
                players.getTurn() != null ? players.getTurn().getName() : "none");
    }

    public boolean playDebugRound(Spot newSpot, DiceState diceState) {
        return playRound(newSpot, diceState);
    }

    public void resetTransientTurnState() {
        prevTurnResult = null;
    }

    public boolean resumeContinuation(TurnContinuationState continuationState) {
        if (continuationState == null) {
            return false;
        }
        return switch (continuationState.completionAction()) {
            case NONE -> true;
            case APPLY_TURN_FOLLOW_UP -> {
                DiceState diceState = dices.getValue() != null ? dices.getValue().diceState() : DiceState.NOREROLL;
                doTurnEndEvent(diceState);
                yield true;
            }
            case END_TURN_WITH_SWITCH -> {
                endRound(true);
                yield true;
            }
            case END_TURN_WITHOUT_SWITCH -> {
                endRound(false);
                yield true;
            }
        };
    }

    private void playRound(DiceValue diceValue) {
        Player turnPlayer = players.getTurn();
        log.debug("Playing round for player {} with diceState={} value={}",
                turnPlayer.getName(), diceValue.diceState(), diceValue.value());
        if (turnPlayer.isInJail()) {
            ((JailSpot) turnPlayer.getSpot()).handleInJailTurn(
                    turnPlayer,
                    diceValue,
                    (request, onResolved) -> hooks.handlePaymentRequest(request, null, onResolved),
                    () -> applyTurnPlan(turnEngine.endTurn(false)),
                    () -> applyTurnPlan(turnEngine.endTurn(true))
            );
            return;
        }
        if (DiceState.JAIL.equals(diceValue.diceState())) {
            prevTurnResult = TurnResult.builder().shouldGoToJail(true).build();
        }
        applyTurnPlan(turnEngine.createMovementPlan(turnPlayer, board, diceValue));
    }

    private void addAnimationAndHandleSpot(Path path, CallbackAction onAnimationEnd) {
        PlayerToken turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, path, onAnimationEnd));
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        if (newSpot.equals(turnPlayer.getSpot())) {
            runtime.popupService().show(text("game.move.sameSpot"));
            return false;
        }
        if (turnPlayer.isInJail()) {
            runtime.popupService().show(text("game.move.inJail"));
            return false;
        }
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        log.trace("Animating player {} along path to {} with diceState={}",
                turnPlayer.getName(), path.getLastSpot().getSpotType(), diceState);
        addAnimationAndHandleSpot(path, () -> {
            CallbackAction completeTurnMove = () -> {
                turnPlayer.setSpot(path.getLastSpot());
                log.trace("Player {} arrived at {}", turnPlayer.getName(), path.getLastSpot().getSpotType());
                handleSpotLogic(diceState, path.getLastSpot());
            };
            if (path.passesGoSpot()) {
                int goMoneyAmount = goMoneyAmountSupplier.getAsInt();
                log.info("Player {} passed GO", turnPlayer.getName());
                runtime.popupService().show(text("game.go.reward", goMoneyAmount), () -> {
                    turnPlayer.addMoney(goMoneyAmount);
                    completeTurnMove.doAction();
                });
                return;
            }
            completeTurnMove.doAction();
        });
        return true;
    }

    private void handleSpotLogic(DiceState diceState, Spot spot) {
        log.debug("Handling spot logic for player {} on spot {} with diceState={}",
                players.getTurn().getName(), spot.getSpotType(), diceState);
        GameState gameState = new GameState(players, dices, board, TurnResult.copyOf(prevTurnResult), new GameFlowPaymentHandler(hooks), propertyPurchaseFlow);
        prevTurnResult = null;
        prevTurnResult = spot.handleTurn(gameState, () -> doTurnEndEvent(diceState));
        log.trace("Spot handling produced prevTurnResult={}", prevTurnResult);
    }

    private void doTurnEndEvent(DiceState diceState) {
        log.trace("Running turn end event for player {} with diceState={} prevTurnResult={}",
                players.getTurn().getName(), diceState, prevTurnResult);
        applyTurnPlan(turnEngine.createFollowUpPlan(players.getTurn(), board, prevTurnResult, diceState));
    }

    private void applyTurnPlan(TurnPlan turnPlan) {
        log.debug("Applying turn plan phase={} effectCount={}", turnPlan.phase(), turnPlan.effects().size());
        for (TurnEffect effect : turnPlan.effects()) {
            log.trace("Applying turn effect {}", effect.getClass().getSimpleName());
            if (effect instanceof MovePlayerEffect movePlayerEffect) {
                playRound(movePlayerEffect.path(), movePlayerEffect.diceState());
            } else if (effect instanceof ShowDiceEffect) {
                hooks.showRollDiceControl();
            } else if (effect instanceof ShowEndTurnEffect) {
                hooks.showEndTurnControl();
            } else if (effect instanceof EndTurnEffect endTurnEffect) {
                endRound(endTurnEffect.switchTurns());
            } else {
                throw new IllegalStateException("Unhandled turn effect: " + effect.getClass().getSimpleName());
            }
        }
    }

    public interface Hooks {
        void updateLogTurnContext();

        boolean gameOver();

        boolean debtActive();

        void hidePrimaryTurnControls();

        void showRollDiceControl();

        void showEndTurnControl();

        void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved);
    }

    private static final class GameFlowPaymentHandler implements fi.monopoly.components.payment.PaymentHandler {
        private final Hooks hooks;

        private GameFlowPaymentHandler(Hooks hooks) {
            this.hooks = hooks;
        }

        @Override
        public void requestPayment(PaymentRequest request, CallbackAction onResolved) {
            hooks.handlePaymentRequest(request, null, onResolved);
        }

        @Override
        public void requestPayment(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
            hooks.handlePaymentRequest(request, continuationState, onResolved);
        }
    }
}
