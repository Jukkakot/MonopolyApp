package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.presentation.game.desktop.assembly.GameDesktopHostFactory;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtController;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Adapts the current {@link Game} host into the callback surface expected by
 * {@link GameDesktopHostFactory}.
 *
 * <p>The host factory only needs read/write access to the live desktop host collaborators. Keeping
 * that translation in one adapter removes a large anonymous wiring block from {@code Game} and
 * makes the remaining composition root easier to shrink further.</p>
 */
final class GameDesktopHostHooksAdapter implements GameDesktopHostFactory.Hooks {
    private final MonopolyRuntime runtime;
    private final Game game;

    GameDesktopHostHooksAdapter(MonopolyRuntime runtime, Game game) {
        this.runtime = runtime;
        this.game = game;
    }

    @Override
    public Supplier<GameSessionState> sessionStateSupplier() {
        return game::sessionStateRef;
    }

    @Override
    public Supplier<Players> playersSupplier() {
        return game::players;
    }

    @Override
    public Supplier<Player> currentTurnPlayerSupplier() {
        return game::currentTurnPlayer;
    }

    @Override
    public Function<String, Player> playerByIdResolver() {
        return game::playerById;
    }

    @Override
    public Supplier<Board> boardSupplier() {
        return game::getBoard;
    }

    @Override
    public Supplier<Dices> dicesSupplier() {
        return game::dices;
    }

    @Override
    public Supplier<Animations> animationsSupplier() {
        return game::animations;
    }

    @Override
    public Supplier<DebtController> debtControllerSupplier() {
        return game::debtController;
    }

    @Override
    public Supplier<DebtState> debtStateSupplier() {
        return game::currentDebtState;
    }

    @Override
    public Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier() {
        return game::gameTurnFlowCoordinatorRef;
    }

    @Override
    public Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier() {
        return game::gamePrimaryTurnControlsRef;
    }

    @Override
    public Supplier<GameSessionQueries> gameSessionQueriesSupplier() {
        return game::gameSessionQueriesRef;
    }

    @Override
    public Supplier<SessionApplicationService> sessionApplicationServiceSupplier() {
        return game::sessionApplicationServiceRef;
    }

    @Override
    public Supplier<GameView> currentGameViewSupplier() {
        return game::currentGameView;
    }

    @Override
    public Supplier<PlayerView> currentPlayerViewSupplier() {
        return game::currentPlayerView;
    }

    @Override
    public Runnable refreshLabelsAction() {
        return game::refreshLabels;
    }

    @Override
    public Runnable rollDiceAction() {
        return game::rollDice;
    }

    @Override
    public BiConsumer<Board, Players> setupDefaultGameStateAction() {
        return (board, players) -> game.setupDefaultGameState(runtime, board, players);
    }

    @Override
    public Runnable hidePrimaryTurnControlsAction() {
        return game::hidePrimaryTurnControls;
    }

    @Override
    public Runnable showRollDiceControlAction() {
        return game::showRollDiceControl;
    }

    @Override
    public Runnable showEndTurnControlAction() {
        return game::showEndTurnControl;
    }

    @Override
    public Runnable updateDebtButtonsAction() {
        return game::updateDebtButtons;
    }

    @Override
    public Runnable syncTransientPresentationStateAction() {
        return game::syncTransientPresentationState;
    }

    @Override
    public Runnable updateLogTurnContextAction() {
        return game::updateLogTurnContext;
    }

    @Override
    public Runnable retryPendingDebtPaymentAction() {
        return game::retryPendingDebtPaymentAction;
    }

    @Override
    public GameDesktopShellDependencies.PaymentRequestHandler paymentRequestHandler() {
        return game::handlePaymentRequest;
    }

    @Override
    public Consumer<Boolean> endRoundAction() {
        return game::endRound;
    }

    @Override
    public GameDesktopShellDependencies.ScheduleNextComputerAction scheduleNextComputerAction() {
        return game::scheduleNextComputerAction;
    }

    @Override
    public Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction() {
        return game::resumeContinuation;
    }

    @Override
    public Consumer<Player> focusPlayerAction() {
        return game::focusPlayer;
    }

    @Override
    public IntSupplier goMoneyAmountSupplier() {
        return game::goMoneyAmountRef;
    }

    @Override
    public BooleanSupplier retryDebtVisibleSupplier() {
        return game::retryDebtVisible;
    }

    @Override
    public BooleanSupplier declareBankruptcyVisibleSupplier() {
        return game::declareBankruptcyVisible;
    }

    @Override
    public BooleanSupplier endRoundVisibleSupplier() {
        return game::endRoundVisible;
    }

    @Override
    public BooleanSupplier rollDiceVisibleSupplier() {
        return game::rollDiceVisible;
    }

    @Override
    public Supplier<MonopolyEventListener> eventListenerSupplier() {
        return () -> game;
    }
}
