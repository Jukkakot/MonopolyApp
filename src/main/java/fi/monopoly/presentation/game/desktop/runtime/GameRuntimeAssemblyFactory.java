package fi.monopoly.presentation.game.desktop.runtime;

import fi.monopoly.application.session.StartingOrderDeterminer;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.text.UiTexts;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Builds and wires the live desktop runtime objects that still back the legacy game shell.
 */
@RequiredArgsConstructor
public final class GameRuntimeAssemblyFactory {
    private final LegacyGameRuntimeBootstrapper legacyGameRuntimeBootstrapper;

    public GameRuntimeAssemblyFactory() {
        this(new LegacyGameRuntimeBootstrapper());
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
            applyDiceRollStartOrder(players);
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

    private static void applyDiceRollStartOrder(Players players) {
        List<Player> playerList = players != null ? players.getPlayers() : List.of();
        if (playerList.size() <= 1) {
            return;
        }
        List<Integer> indices = IntStream.range(0, playerList.size()).boxed().toList();
        List<Integer> ordered = StartingOrderDeterminer.determineStartOrder(indices, new Random());
        players.restoreTurn(playerList.get(ordered.get(0)));
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
