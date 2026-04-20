package fi.monopoly.presentation.game.desktop;

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
import fi.monopoly.presentation.game.BotTurnScheduler;
import fi.monopoly.presentation.game.GameSessionQueries;
import fi.monopoly.presentation.game.GameSessionState;
import fi.monopoly.presentation.game.GameTurnFlowCoordinator;
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
    private final Supplier<GameSessionState> sessionStateSupplier;
    private final Supplier<Players> playersSupplier;
    private final Supplier<Player> currentTurnPlayerSupplier;
    private final java.util.function.Function<String, Player> playerByIdResolver;
    private final Supplier<Board> boardSupplier;
    private final Supplier<Dices> dicesSupplier;
    private final Supplier<Animations> animationsSupplier;
    private final Supplier<DebtController> debtControllerSupplier;
    private final Supplier<DebtState> debtStateSupplier;
    private final Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier;
    private final Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier;
    private final Supplier<GameSessionQueries> gameSessionQueriesSupplier;
    private final Supplier<SessionApplicationService> sessionApplicationServiceSupplier;
    private final Supplier<PopupService> popupServiceSupplier;
    private final Supplier<BotTurnScheduler> botTurnSchedulerSupplier;
    private final Supplier<GameView> currentGameViewSupplier;
    private final Supplier<PlayerView> currentPlayerViewSupplier;
    private final Runnable refreshLabelsAction;
    private final Runnable rollDiceAction;
    private final BiConsumer<Board, Players> setupDefaultGameStateAction;
    private final Runnable hidePrimaryTurnControlsAction;
    private final Runnable showRollDiceControlAction;
    private final Runnable showEndTurnControlAction;
    private final Runnable updateDebtButtonsAction;
    private final Runnable syncTransientPresentationStateAction;
    private final Runnable updateLogTurnContextAction;
    private final Runnable retryPendingDebtPaymentAction;
    private final PaymentRequestHandler paymentRequestHandler;
    private final Consumer<Boolean> endRoundAction;
    private final ScheduleNextComputerAction scheduleNextComputerAction;
    private final Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction;
    private final Consumer<Player> focusPlayerAction;
    private final IntSupplier goMoneyAmountSupplier;
    private final BooleanSupplier retryDebtVisibleSupplier;
    private final BooleanSupplier declareBankruptcyVisibleSupplier;
    private final BooleanSupplier endRoundVisibleSupplier;
    private final BooleanSupplier rollDiceVisibleSupplier;
    private final Supplier<MonopolyEventListener> eventListenerSupplier;

    public GameDesktopShellDependencies(
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
            Supplier<BotTurnScheduler> botTurnSchedulerSupplier,
            Supplier<GameView> currentGameViewSupplier,
            Supplier<PlayerView> currentPlayerViewSupplier,
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
            Consumer<Player> focusPlayerAction,
            IntSupplier goMoneyAmountSupplier,
            BooleanSupplier retryDebtVisibleSupplier,
            BooleanSupplier declareBankruptcyVisibleSupplier,
            BooleanSupplier endRoundVisibleSupplier,
            BooleanSupplier rollDiceVisibleSupplier,
            Supplier<MonopolyEventListener> eventListenerSupplier
    ) {
        this.sessionStateSupplier = sessionStateSupplier;
        this.playersSupplier = playersSupplier;
        this.currentTurnPlayerSupplier = currentTurnPlayerSupplier;
        this.playerByIdResolver = playerByIdResolver;
        this.boardSupplier = boardSupplier;
        this.dicesSupplier = dicesSupplier;
        this.animationsSupplier = animationsSupplier;
        this.debtControllerSupplier = debtControllerSupplier;
        this.debtStateSupplier = debtStateSupplier;
        this.gameTurnFlowCoordinatorSupplier = gameTurnFlowCoordinatorSupplier;
        this.gamePrimaryTurnControlsSupplier = gamePrimaryTurnControlsSupplier;
        this.gameSessionQueriesSupplier = gameSessionQueriesSupplier;
        this.sessionApplicationServiceSupplier = sessionApplicationServiceSupplier;
        this.popupServiceSupplier = popupServiceSupplier;
        this.botTurnSchedulerSupplier = botTurnSchedulerSupplier;
        this.currentGameViewSupplier = currentGameViewSupplier;
        this.currentPlayerViewSupplier = currentPlayerViewSupplier;
        this.refreshLabelsAction = refreshLabelsAction;
        this.rollDiceAction = rollDiceAction;
        this.setupDefaultGameStateAction = setupDefaultGameStateAction;
        this.hidePrimaryTurnControlsAction = hidePrimaryTurnControlsAction;
        this.showRollDiceControlAction = showRollDiceControlAction;
        this.showEndTurnControlAction = showEndTurnControlAction;
        this.updateDebtButtonsAction = updateDebtButtonsAction;
        this.syncTransientPresentationStateAction = syncTransientPresentationStateAction;
        this.updateLogTurnContextAction = updateLogTurnContextAction;
        this.retryPendingDebtPaymentAction = retryPendingDebtPaymentAction;
        this.paymentRequestHandler = paymentRequestHandler;
        this.endRoundAction = endRoundAction;
        this.scheduleNextComputerAction = scheduleNextComputerAction;
        this.resumeContinuationAction = resumeContinuationAction;
        this.focusPlayerAction = focusPlayerAction;
        this.goMoneyAmountSupplier = goMoneyAmountSupplier;
        this.retryDebtVisibleSupplier = retryDebtVisibleSupplier;
        this.declareBankruptcyVisibleSupplier = declareBankruptcyVisibleSupplier;
        this.endRoundVisibleSupplier = endRoundVisibleSupplier;
        this.rollDiceVisibleSupplier = rollDiceVisibleSupplier;
        this.eventListenerSupplier = eventListenerSupplier;
    }

    @Override
    public GameSessionState sessionState() {
        return sessionStateSupplier.get();
    }

    @Override
    public Players players() {
        return playersSupplier.get();
    }

    @Override
    public Player currentTurnPlayer() {
        return currentTurnPlayerSupplier.get();
    }

    @Override
    public Player playerById(String playerId) {
        return playerByIdResolver.apply(playerId);
    }

    @Override
    public Board board() {
        return boardSupplier.get();
    }

    @Override
    public Dices dices() {
        return dicesSupplier.get();
    }

    @Override
    public Animations animations() {
        return animationsSupplier.get();
    }

    @Override
    public DebtController debtController() {
        return debtControllerSupplier.get();
    }

    @Override
    public DebtState debtState() {
        return debtStateSupplier.get();
    }

    @Override
    public GameTurnFlowCoordinator gameTurnFlowCoordinator() {
        return gameTurnFlowCoordinatorSupplier.get();
    }

    @Override
    public GamePrimaryTurnControls gamePrimaryTurnControls() {
        return gamePrimaryTurnControlsSupplier.get();
    }

    @Override
    public GameSessionQueries gameSessionQueries() {
        return gameSessionQueriesSupplier.get();
    }

    @Override
    public SessionApplicationService sessionApplicationService() {
        return sessionApplicationServiceSupplier.get();
    }

    @Override
    public PopupService popupService() {
        return popupServiceSupplier.get();
    }

    @Override
    public BotTurnScheduler botTurnScheduler() {
        return botTurnSchedulerSupplier.get();
    }

    @Override
    public GameView createCurrentGameView() {
        return currentGameViewSupplier.get();
    }

    @Override
    public PlayerView createCurrentPlayerView() {
        return currentPlayerViewSupplier.get();
    }

    @Override
    public void refreshLabels() {
        refreshLabelsAction.run();
    }

    @Override
    public void rollDice() {
        rollDiceAction.run();
    }

    @Override
    public void setupDefaultGameState(Board board, Players players) {
        setupDefaultGameStateAction.accept(board, players);
    }

    @Override
    public void hidePrimaryTurnControls() {
        hidePrimaryTurnControlsAction.run();
    }

    @Override
    public void showRollDiceControl() {
        showRollDiceControlAction.run();
    }

    @Override
    public void showEndTurnControl() {
        showEndTurnControlAction.run();
    }

    @Override
    public void updateDebtButtons() {
        updateDebtButtonsAction.run();
    }

    @Override
    public void syncTransientPresentationState() {
        syncTransientPresentationStateAction.run();
    }

    @Override
    public void updateLogTurnContext() {
        updateLogTurnContextAction.run();
    }

    @Override
    public void retryPendingDebtPaymentAction() {
        retryPendingDebtPaymentAction.run();
    }

    @Override
    public void handlePaymentRequest(
            PaymentRequest request,
            fi.monopoly.domain.session.TurnContinuationState continuationState,
            CallbackAction onResolved
    ) {
        paymentRequestHandler.handle(request, continuationState, onResolved);
    }

    @Override
    public void endRound(boolean switchTurns) {
        endRoundAction.accept(switchTurns);
    }

    @Override
    public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        scheduleNextComputerAction.schedule(delayKind, now);
    }

    @Override
    public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        resumeContinuationAction.accept(continuationState);
    }

    @Override
    public void focusPlayer(Player player) {
        focusPlayerAction.accept(player);
    }

    @Override
    public int goMoneyAmount() {
        return goMoneyAmountSupplier.getAsInt();
    }

    @Override
    public boolean retryDebtVisible() {
        return retryDebtVisibleSupplier.getAsBoolean();
    }

    @Override
    public boolean declareBankruptcyVisible() {
        return declareBankruptcyVisibleSupplier.getAsBoolean();
    }

    @Override
    public boolean endRoundVisible() {
        return endRoundVisibleSupplier.getAsBoolean();
    }

    @Override
    public boolean rollDiceVisible() {
        return rollDiceVisibleSupplier.getAsBoolean();
    }

    @Override
    public MonopolyEventListener eventListener() {
        return eventListenerSupplier.get();
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
