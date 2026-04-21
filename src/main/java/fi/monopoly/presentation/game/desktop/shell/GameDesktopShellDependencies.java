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
public final class GameDesktopShellDependencies implements GameDesktopShellCoordinator.Dependencies {
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

    @Override
    public GameSessionState sessionState() {
        return stateAccess.sessionStateSupplier().get();
    }

    @Override
    public Players players() {
        return stateAccess.playersSupplier().get();
    }

    @Override
    public Player currentTurnPlayer() {
        return stateAccess.currentTurnPlayerSupplier().get();
    }

    @Override
    public Player playerById(String playerId) {
        return stateAccess.playerByIdResolver().apply(playerId);
    }

    @Override
    public Board board() {
        return stateAccess.boardSupplier().get();
    }

    @Override
    public Dices dices() {
        return stateAccess.dicesSupplier().get();
    }

    @Override
    public Animations animations() {
        return stateAccess.animationsSupplier().get();
    }

    @Override
    public DebtController debtController() {
        return stateAccess.debtControllerSupplier().get();
    }

    @Override
    public DebtState debtState() {
        return stateAccess.debtStateSupplier().get();
    }

    @Override
    public GameTurnFlowCoordinator gameTurnFlowCoordinator() {
        return stateAccess.gameTurnFlowCoordinatorSupplier().get();
    }

    @Override
    public GamePrimaryTurnControls gamePrimaryTurnControls() {
        return stateAccess.gamePrimaryTurnControlsSupplier().get();
    }

    @Override
    public GameSessionQueries gameSessionQueries() {
        return stateAccess.gameSessionQueriesSupplier().get();
    }

    @Override
    public SessionApplicationService sessionApplicationService() {
        return stateAccess.sessionApplicationServiceSupplier().get();
    }

    @Override
    public PopupService popupService() {
        return stateAccess.popupServiceSupplier().get();
    }

    @Override
    public BotTurnScheduler botTurnScheduler() {
        return stateAccess.botTurnSchedulerSupplier().get();
    }

    @Override
    public GameView createCurrentGameView() {
        return projectionAccess.currentGameViewSupplier().get();
    }

    @Override
    public PlayerView createCurrentPlayerView() {
        return projectionAccess.currentPlayerViewSupplier().get();
    }

    @Override
    public void refreshLabels() {
        actionAccess.refreshLabelsAction().run();
    }

    @Override
    public void rollDice() {
        actionAccess.rollDiceAction().run();
    }

    @Override
    public void setupDefaultGameState(Board board, Players players) {
        actionAccess.setupDefaultGameStateAction().accept(board, players);
    }

    @Override
    public void hidePrimaryTurnControls() {
        actionAccess.hidePrimaryTurnControlsAction().run();
    }

    @Override
    public void showRollDiceControl() {
        actionAccess.showRollDiceControlAction().run();
    }

    @Override
    public void showEndTurnControl() {
        actionAccess.showEndTurnControlAction().run();
    }

    @Override
    public void updateDebtButtons() {
        actionAccess.updateDebtButtonsAction().run();
    }

    @Override
    public void syncTransientPresentationState() {
        actionAccess.syncTransientPresentationStateAction().run();
    }

    @Override
    public void updateLogTurnContext() {
        actionAccess.updateLogTurnContextAction().run();
    }

    @Override
    public void retryPendingDebtPaymentAction() {
        actionAccess.retryPendingDebtPaymentAction().run();
    }

    @Override
    public void handlePaymentRequest(
            PaymentRequest request,
            fi.monopoly.domain.session.TurnContinuationState continuationState,
            CallbackAction onResolved
    ) {
        actionAccess.paymentRequestHandler().handle(request, continuationState, onResolved);
    }

    @Override
    public void endRound(boolean switchTurns) {
        actionAccess.endRoundAction().accept(switchTurns);
    }

    @Override
    public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        actionAccess.scheduleNextComputerAction().schedule(delayKind, now);
    }

    @Override
    public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        actionAccess.resumeContinuationAction().accept(continuationState);
    }

    @Override
    public void focusPlayer(Player player) {
        actionAccess.focusPlayerAction().accept(player);
    }

    @Override
    public int goMoneyAmount() {
        return visibilityAccess.goMoneyAmountSupplier().getAsInt();
    }

    @Override
    public boolean retryDebtVisible() {
        return visibilityAccess.retryDebtVisibleSupplier().getAsBoolean();
    }

    @Override
    public boolean declareBankruptcyVisible() {
        return visibilityAccess.declareBankruptcyVisibleSupplier().getAsBoolean();
    }

    @Override
    public boolean endRoundVisible() {
        return visibilityAccess.endRoundVisibleSupplier().getAsBoolean();
    }

    @Override
    public boolean rollDiceVisible() {
        return visibilityAccess.rollDiceVisibleSupplier().getAsBoolean();
    }

    @Override
    public MonopolyEventListener eventListener() {
        return visibilityAccess.eventListenerSupplier().get();
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
            Supplier<GameView> currentGameViewSupplier,
            Supplier<PlayerView> currentPlayerViewSupplier
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
            Supplier<MonopolyEventListener> eventListenerSupplier
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
