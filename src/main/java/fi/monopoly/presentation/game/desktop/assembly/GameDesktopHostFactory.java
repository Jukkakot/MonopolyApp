package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.desktop.session.LocalSessionActions;
import fi.monopoly.presentation.game.desktop.session.SessionViewFacade;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.ui.GameDesktopControlsFactory;
import fi.monopoly.presentation.game.desktop.ui.GameFrameCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarPresenter;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarStateFactory;
import fi.monopoly.presentation.game.desktop.ui.GameUiController;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.utils.DebugPerformanceStats;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Builds the desktop host wiring that still lives around the extracted runtime and shell
 * coordinators.
 *
 * <p>{@code Game} remains the compatibility-facing host object for tests and the current desktop
 * application, but most of its constructor work is now grouped here as one explicit assembly step.
 * That makes it easier to continue shrinking the host class without changing its public surface
 * area yet.</p>
 */
public final class GameDesktopHostFactory {
    private final GameDesktopAssemblyFactory gameDesktopAssemblyFactory;

    public GameDesktopHostFactory() {
        this(new GameDesktopAssemblyFactory());
    }

    GameDesktopHostFactory(GameDesktopAssemblyFactory gameDesktopAssemblyFactory) {
        this.gameDesktopAssemblyFactory = gameDesktopAssemblyFactory;
    }

    public GameDesktopHostContext create(Config config, Hooks hooks) {
        GameDesktopShellCoordinator shellCoordinator = new GameDesktopShellCoordinator(
                config.runtime(),
                config.sessionId(),
                config.supportedLocales(),
                config.localSessionActions(),
                config.gameSessionStateCoordinator(),
                config.gameBotTurnControlCoordinator()
        );

        GameDesktopShellDependencies shellDependencies = new GameDesktopShellDependencies(
                hooks.sessionStateSupplier(),
                hooks.playersSupplier(),
                hooks.currentTurnPlayerSupplier(),
                hooks.playerByIdResolver(),
                hooks.boardSupplier(),
                hooks.dicesSupplier(),
                hooks.animationsSupplier(),
                hooks.debtControllerSupplier(),
                hooks.debtStateSupplier(),
                hooks.gameTurnFlowCoordinatorSupplier(),
                hooks.gamePrimaryTurnControlsSupplier(),
                hooks.gameSessionQueriesSupplier(),
                hooks.sessionApplicationServiceSupplier(),
                () -> config.runtime().popupService(),
                () -> config.botTurnScheduler(),
                hooks.currentGameViewSupplier(),
                hooks.currentPlayerViewSupplier(),
                hooks.refreshLabelsAction(),
                hooks.rollDiceAction(),
                hooks.setupDefaultGameStateAction(),
                hooks.hidePrimaryTurnControlsAction(),
                hooks.showRollDiceControlAction(),
                hooks.showEndTurnControlAction(),
                hooks.updateDebtButtonsAction(),
                hooks.syncTransientPresentationStateAction(),
                hooks.updateLogTurnContextAction(),
                hooks.retryPendingDebtPaymentAction(),
                hooks.paymentRequestHandler(),
                hooks.endRoundAction(),
                hooks.scheduleNextComputerAction(),
                hooks.resumeContinuationAction(),
                hooks.focusPlayerAction(),
                hooks.goMoneyAmountSupplier(),
                hooks.retryDebtVisibleSupplier(),
                hooks.declareBankruptcyVisibleSupplier(),
                hooks.endRoundVisibleSupplier(),
                hooks.rollDiceVisibleSupplier(),
                hooks.eventListenerSupplier()
        );

        GameDesktopAssemblyFactory.GameDesktopAssembly desktopAssembly = gameDesktopAssemblyFactory.create(
                config.runtime(),
                config.restoredSessionState(),
                config.sessionId(),
                config.desktopControls().buttons(),
                config.turnEngine(),
                shellCoordinator.createRuntimeAssemblyHooks(shellDependencies),
                shellCoordinator.createSessionBridgeHooks(shellDependencies),
                shellCoordinator.createPresentationHooks(shellDependencies),
                config.debugPerformanceStats()
        );

        GameFrameCoordinator gameFrameCoordinator = new GameFrameCoordinator(
                config.runtime(),
                desktopAssembly.gameControlLayout(),
                new GameSidebarPresenter(config.runtime()),
                desktopAssembly.gamePresentationSupport(),
                desktopAssembly.gamePrimaryTurnControls(),
                new GameSidebarStateFactory(),
                config.gameSessionStateCoordinator(),
                config.botTurnDriver(),
                config.botTurnScheduler(),
                config.debugPerformanceStats(),
                config.desktopControls().allButtons()
        );

        return new GameDesktopHostContext(
                shellCoordinator,
                shellDependencies,
                desktopAssembly.board(),
                desktopAssembly.players(),
                desktopAssembly.dices(),
                desktopAssembly.animations(),
                desktopAssembly.debtController(),
                desktopAssembly.debugController(),
                desktopAssembly.sessionApplicationService(),
                desktopAssembly.debtActionDispatcher(),
                null,
                desktopAssembly.gameControlLayout(),
                desktopAssembly.gamePrimaryTurnControls(),
                desktopAssembly.gameSessionQueries(),
                desktopAssembly.gameTurnFlowCoordinator(),
                desktopAssembly.gameUiController(),
                desktopAssembly.gameBotTurnHooks(),
                gameFrameCoordinator
        );
    }

    public record Config(
            MonopolyRuntime runtime,
            SessionState restoredSessionState,
            LocalSessionActions localSessionActions,
            String sessionId,
            List<Locale> supportedLocales,
            fi.monopoly.components.turn.TurnEngine turnEngine,
            GameSessionStateCoordinator gameSessionStateCoordinator,
            GameBotTurnControlCoordinator gameBotTurnControlCoordinator,
            GameBotTurnDriver botTurnDriver,
            BotTurnScheduler botTurnScheduler,
            DebugPerformanceStats debugPerformanceStats,
            GameDesktopControlsFactory.GameDesktopControls desktopControls
    ) {
    }

    public record GameDesktopHostContext(
            GameDesktopShellCoordinator shellCoordinator,
            GameDesktopShellDependencies shellDependencies,
            Board board,
            Players players,
            Dices dices,
            Animations animations,
            DebtController debtController,
            DebugController debugController,
            SessionApplicationService sessionApplicationService,
            DebtActionDispatcher debtActionDispatcher,
            SessionViewFacade sessionViewFacade,
            fi.monopoly.presentation.game.desktop.ui.GameControlLayout gameControlLayout,
            GamePrimaryTurnControls gamePrimaryTurnControls,
            GameSessionQueries gameSessionQueries,
            GameTurnFlowCoordinator gameTurnFlowCoordinator,
            GameUiController gameUiController,
            GameBotTurnDriver.Hooks gameBotTurnHooks,
            GameFrameCoordinator gameFrameCoordinator
    ) {
    }

    public interface Hooks {
        Supplier<GameSessionState> sessionStateSupplier();

        Supplier<Players> playersSupplier();

        Supplier<Player> currentTurnPlayerSupplier();

        Function<String, Player> playerByIdResolver();

        Supplier<Board> boardSupplier();

        Supplier<Dices> dicesSupplier();

        Supplier<Animations> animationsSupplier();

        Supplier<DebtController> debtControllerSupplier();

        Supplier<DebtState> debtStateSupplier();

        Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier();

        Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier();

        Supplier<GameSessionQueries> gameSessionQueriesSupplier();

        Supplier<SessionApplicationService> sessionApplicationServiceSupplier();

        Supplier<GameView> currentGameViewSupplier();

        Supplier<PlayerView> currentPlayerViewSupplier();

        Runnable refreshLabelsAction();

        Runnable rollDiceAction();

        BiConsumer<Board, Players> setupDefaultGameStateAction();

        Runnable hidePrimaryTurnControlsAction();

        Runnable showRollDiceControlAction();

        Runnable showEndTurnControlAction();

        Runnable updateDebtButtonsAction();

        Runnable syncTransientPresentationStateAction();

        Runnable updateLogTurnContextAction();

        Runnable retryPendingDebtPaymentAction();

        GameDesktopShellDependencies.PaymentRequestHandler paymentRequestHandler();

        Consumer<Boolean> endRoundAction();

        GameDesktopShellDependencies.ScheduleNextComputerAction scheduleNextComputerAction();

        Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction();

        Consumer<Player> focusPlayerAction();

        IntSupplier goMoneyAmountSupplier();

        BooleanSupplier retryDebtVisibleSupplier();

        BooleanSupplier declareBankruptcyVisibleSupplier();

        BooleanSupplier endRoundVisibleSupplier();

        BooleanSupplier rollDiceVisibleSupplier();

        Supplier<MonopolyEventListener> eventListenerSupplier();
    }
}
