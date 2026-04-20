package fi.monopoly.presentation.game.desktop.runtime;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.text.UiTexts;

/**
 * Builds and wires the live desktop runtime objects that still back the legacy game shell.
 */
public final class GameRuntimeAssemblyFactory {
    private final LegacyGameRuntimeBootstrapper legacyGameRuntimeBootstrapper;

    public GameRuntimeAssemblyFactory() {
        this(new LegacyGameRuntimeBootstrapper());
    }

    GameRuntimeAssemblyFactory(LegacyGameRuntimeBootstrapper legacyGameRuntimeBootstrapper) {
        this.legacyGameRuntimeBootstrapper = legacyGameRuntimeBootstrapper;
    }

    public GameRuntimeAssembly create(
            MonopolyRuntime runtime,
            SessionState restoredSessionState,
            Hooks hooks
    ) {
        UiTexts.addChangeListener(hooks::refreshLabels);
        runtime.eventBus().addListener(hooks.eventListener());

        Dices dices = Dices.setRollDice(runtime, hooks::rollDice);
        Animations animations = new Animations();

        LegacyGameRuntimeBootstrapper.LegacyGameRuntimeState runtimeState =
                legacyGameRuntimeBootstrapper.bootstrap(runtime, restoredSessionState);
        Board board = runtimeState.board();
        Players players = runtimeState.players();
        if (!runtimeState.restoredSession()) {
            hooks.setupDefaultGameState(board, players);
        }

        DebtController debtController = new DebtController(
                runtime,
                players,
                hooks::hidePrimaryTurnControls,
                hooks::showRollDiceControl,
                hooks::onDebtStateChanged,
                hooks::declareWinner
        );
        DebugController debugController = new DebugController(
                runtime,
                board,
                () -> players != null ? players.getTurn() : null,
                hooks::debugResetTurnState,
                hooks::restoreNormalTurnControls,
                hooks::retryPendingDebtPaymentAction,
                hooks::handlePaymentRequest
        );

        return new GameRuntimeAssembly(
                board,
                players,
                dices,
                animations,
                debtController,
                debugController,
                runtimeState.restoredSession()
        );
    }

    public void registerGameSession(
            MonopolyRuntime runtime,
            GameRuntimeAssembly assembly,
            DebtActionDispatcher debtActionDispatcher,
            Hooks hooks
    ) {
        runtime.setGameSession(new GameSession(assembly.players(), assembly.dices(), assembly.animations())
                .withStateSuppliers(
                        hooks::debtActive,
                        hooks::gameOver,
                        hooks::goMoneyAmount
                )
                .withDebtActionDispatcher(debtActionDispatcher));
    }

    public interface Hooks {
        void refreshLabels();

        MonopolyEventListener eventListener();

        void rollDice();

        void setupDefaultGameState(Board board, Players players);

        void hidePrimaryTurnControls();

        void showRollDiceControl();

        void onDebtStateChanged();

        void declareWinner(Player winner);

        void debugResetTurnState();

        void restoreNormalTurnControls();

        void retryPendingDebtPaymentAction();

        void handlePaymentRequest(fi.monopoly.components.payment.PaymentRequest request, CallbackAction onResolved);

        boolean debtActive();

        boolean gameOver();

        int goMoneyAmount();
    }

    public record GameRuntimeAssembly(
            Board board,
            Players players,
            Dices dices,
            Animations animations,
            DebtController debtController,
            DebugController debugController,
            boolean restoredSession
    ) {
    }
}
