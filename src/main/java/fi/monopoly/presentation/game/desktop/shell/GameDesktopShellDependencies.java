package fi.monopoly.presentation.game.desktop.shell;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.session.debt.DebtController;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Stable adapter object that exposes the mutable desktop shell collaborators behind supplier-based
 * accessors.
 *
 * <p>{@code Game} mutates many fields during desktop assembly, but the extracted shell
 * coordinators need one durable dependency object. This adapter keeps those coordinators decoupled
 * from the monolithic {@code Game} class while still allowing late-bound access to the live
 * runtime objects.</p>
 */
public final class GameDesktopShellDependencies {
    private final StateAccess stateAccess;
    private final ProjectionAccess projectionAccess;
    private final ActionAccess actionAccess;
    private final VisibilityAccess visibilityAccess;

    public GameDesktopShellDependencies(
            StateAccess stateAccess,
            ProjectionAccess projectionAccess,
            ActionAccess actionAccess,
            VisibilityAccess visibilityAccess
    ) {
        this.stateAccess = stateAccess;
        this.projectionAccess = projectionAccess;
        this.actionAccess = actionAccess;
        this.visibilityAccess = visibilityAccess;
    }

    public GameSessionState sessionState() {
        return stateAccess.sessionStateSupplier().get();
    }

    public Players players() {
        return stateAccess.playersSupplier().get();
    }

    public Player currentTurnPlayer() {
        return stateAccess.currentTurnPlayerSupplier().get();
    }

    public Player playerById(String playerId) {
        return stateAccess.playerByIdResolver().apply(playerId);
    }

    public Board board() {
        return stateAccess.boardSupplier().get();
    }

    public Dices dices() {
        return stateAccess.dicesSupplier().get();
    }

    public Animations animations() {
        return stateAccess.animationsSupplier().get();
    }

    public DebtController debtController() {
        return stateAccess.debtControllerSupplier().get();
    }

    public DebtState debtState() {
        return stateAccess.debtStateSupplier().get();
    }

    public GameTurnFlowCoordinator gameTurnFlowCoordinator() {
        return stateAccess.gameTurnFlowCoordinatorSupplier().get();
    }

    public GamePrimaryTurnControls gamePrimaryTurnControls() {
        return stateAccess.gamePrimaryTurnControlsSupplier().get();
    }

    public GameSessionQueries gameSessionQueries() {
        return stateAccess.gameSessionQueriesSupplier().get();
    }

    public SessionApplicationService sessionApplicationService() {
        return stateAccess.sessionApplicationServiceSupplier().get();
    }

    public PopupService popupService() {
        return stateAccess.popupServiceSupplier().get();
    }

    public BotTurnScheduler botTurnScheduler() {
        return stateAccess.botTurnSchedulerSupplier().get();
    }

    public GameView createGameViewFor(Player player) {
        return projectionAccess.currentGameViewFactory().apply(player);
    }

    public PlayerView createPlayerViewFor(Player player) {
        return projectionAccess.currentPlayerViewFactory().apply(player);
    }

    public void refreshLabels() {
        actionAccess.refreshLabelsAction().run();
    }

    public void rollDice() {
        actionAccess.rollDiceAction().run();
    }

    public void setupDefaultGameState(Board board, Players players) {
        actionAccess.setupDefaultGameStateAction().accept(board, players);
    }

    public void hidePrimaryTurnControls() {
        actionAccess.hidePrimaryTurnControlsAction().run();
    }

    public void showRollDiceControl() {
        actionAccess.showRollDiceControlAction().run();
    }

    public void showEndTurnControl() {
        actionAccess.showEndTurnControlAction().run();
    }

    public void updateDebtButtons() {
        actionAccess.updateDebtButtonsAction().run();
    }

    public void syncTransientPresentationState() {
        actionAccess.syncTransientPresentationStateAction().run();
    }

    public void updateLogTurnContext() {
        actionAccess.updateLogTurnContextAction().run();
    }

    public void retryPendingDebtPaymentAction() {
        actionAccess.retryPendingDebtPaymentAction().run();
    }

    public void handlePaymentRequest(
            PaymentRequest request,
            fi.monopoly.domain.session.TurnContinuationState continuationState,
            CallbackAction onResolved
    ) {
        actionAccess.paymentRequestHandler().handle(request, continuationState, onResolved);
    }

    public void endRound(boolean switchTurns) {
        actionAccess.endRoundAction().accept(switchTurns);
    }

    public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        actionAccess.scheduleNextComputerAction().schedule(delayKind, now);
    }

    public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        actionAccess.resumeContinuationAction().accept(continuationState);
    }

    public void focusPlayer(Player player) {
        actionAccess.focusPlayerAction().accept(player);
    }

    public int goMoneyAmount() {
        return visibilityAccess.goMoneyAmountSupplier().getAsInt();
    }

    public boolean retryDebtVisible() {
        return visibilityAccess.retryDebtVisibleSupplier().getAsBoolean();
    }

    public boolean declareBankruptcyVisible() {
        return visibilityAccess.declareBankruptcyVisibleSupplier().getAsBoolean();
    }

    public boolean endRoundVisible() {
        return visibilityAccess.endRoundVisibleSupplier().getAsBoolean();
    }

    public boolean rollDiceVisible() {
        return visibilityAccess.rollDiceVisibleSupplier().getAsBoolean();
    }

    public MonopolyEventListener eventListener() {
        return visibilityAccess.eventListenerSupplier().get();
    }

    public boolean projectedRollDiceActionAvailable() {
        return visibilityAccess.projectedRollDiceActionAvailableSupplier().getAsBoolean();
    }

    public boolean projectedEndTurnActionAvailable() {
        return visibilityAccess.projectedEndTurnActionAvailableSupplier().getAsBoolean();
    }

    public record StateAccess(
            Supplier<GameSessionState> sessionStateSupplier,
            Supplier<Players> playersSupplier,
            Supplier<Player> currentTurnPlayerSupplier,
            java.util.function.Function<String, Player> playerByIdResolver,
            Supplier<Board> boardSupplier,
            Supplier<Dices> dicesSupplier,
            Supplier<Animations> animationsSupplier,
            Supplier<DebtController> debtControllerSupplier,
            Supplier<DebtState> debtStateSupplier,
            Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier,
            Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier,
            Supplier<GameSessionQueries> gameSessionQueriesSupplier,
            Supplier<SessionApplicationService> sessionApplicationServiceSupplier,
            Supplier<PopupService> popupServiceSupplier,
            Supplier<BotTurnScheduler> botTurnSchedulerSupplier
    ) {
    }

    public record ProjectionAccess(
            java.util.function.Function<Player, GameView> currentGameViewFactory,
            java.util.function.Function<Player, PlayerView> currentPlayerViewFactory
    ) {
    }

    public record ActionAccess(
            Runnable refreshLabelsAction,
            Runnable rollDiceAction,
            BiConsumer<Board, Players> setupDefaultGameStateAction,
            Runnable hidePrimaryTurnControlsAction,
            Runnable showRollDiceControlAction,
            Runnable showEndTurnControlAction,
            Runnable updateDebtButtonsAction,
            Runnable syncTransientPresentationStateAction,
            Runnable updateLogTurnContextAction,
            Runnable retryPendingDebtPaymentAction,
            PaymentRequestHandler paymentRequestHandler,
            Consumer<Boolean> endRoundAction,
            ScheduleNextComputerAction scheduleNextComputerAction,
            Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction,
            Consumer<Player> focusPlayerAction
    ) {
    }

    public record VisibilityAccess(
            IntSupplier goMoneyAmountSupplier,
            BooleanSupplier retryDebtVisibleSupplier,
            BooleanSupplier declareBankruptcyVisibleSupplier,
            BooleanSupplier endRoundVisibleSupplier,
            BooleanSupplier rollDiceVisibleSupplier,
            Supplier<MonopolyEventListener> eventListenerSupplier,
            BooleanSupplier projectedRollDiceActionAvailableSupplier,
            BooleanSupplier projectedEndTurnActionAvailableSupplier
    ) {
    }

    @FunctionalInterface
    public interface PaymentRequestHandler {
        void handle(
                PaymentRequest request,
                fi.monopoly.domain.session.TurnContinuationState continuationState,
                CallbackAction onResolved
        );
    }

    @FunctionalInterface
    public interface ScheduleNextComputerAction {
        void schedule(BotTurnScheduler.DelayKind delayKind, int now);
    }
}
