package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.session.SessionPaymentPort;
import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnControlCoordinator;
import fi.monopoly.host.bot.GameBotTurnDriver;
import fi.monopoly.host.session.local.LocalHostedGameLoopCoordinator;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopPresentationCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopSessionCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.ui.*;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.utils.DebugPerformanceStats;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.function.*;

/**
 * Builds the desktop host wiring that still lives around the extracted runtime and shell
 * coordinators.
 *
 * <p>{@code Game} remains the compatibility-facing host object for tests and the current desktop
 * application, but most of its constructor work is now grouped here as one explicit assembly step.
 * That makes it easier to continue shrinking the host class without changing its public surface
 * area yet.</p>
 */
@RequiredArgsConstructor
public final class GameDesktopHostFactory {
    private final GameDesktopAssemblyFactory gameDesktopAssemblyFactory;

    public GameDesktopHostFactory() {
        this(new GameDesktopAssemblyFactory());
    }

    public GameDesktopHostContext create(Config config, Hooks hooks) {
        GameDesktopSessionCoordinator sessionCoordinator = new GameDesktopSessionCoordinator(
                config.runtime(),
                config.gameSessionStateCoordinator()
        );
        GameDesktopPresentationCoordinator presentationCoordinator = new GameDesktopPresentationCoordinator(
                config.runtime(),
                config.sessionId(),
                config.supportedLocales(),
                config.localSessionActions(),
                config.gameSessionStateCoordinator(),
                config.gameBotTurnControlCoordinator()
        );

        GameDesktopShellDependencies shellDependencies = new GameDesktopShellDependencies(
                new GameDesktopShellDependencies.StateAccess(
                        hooks.sessionStateSupplier(),
                        hooks.playersSupplier(),
                        hooks.hasActiveTurnSupplier(),
                        hooks.isComputerTurnSupplier(),
                        hooks.boardSupplier(),
                        hooks.dicesSupplier(),
                        hooks.animationsSupplier(),
                        hooks.debtControllerSupplier(),
                        hooks.debtStateSupplier(),
                        hooks.gameTurnFlowCoordinatorSupplier(),
                        hooks.gamePrimaryTurnControlsSupplier(),
                        hooks.gameSessionQueriesSupplier(),
                        hooks.sessionCommandPortSupplier(),
                        hooks.sessionPresentationStateSupplier(),
                        () -> config.runtime().popupService(),
                        config::botTurnScheduler
                ),
                new GameDesktopShellDependencies.ProjectionAccess(
                        hooks.currentGameViewFactory(),
                        hooks.currentPlayerViewFactory()
                ),
                new GameDesktopShellDependencies.ActionAccess(
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
                        hooks.focusPlayerByIdAction()
                ),
                new GameDesktopShellDependencies.VisibilityAccess(
                        hooks.goMoneyAmountSupplier(),
                        hooks.retryDebtVisibleSupplier(),
                        hooks.declareBankruptcyVisibleSupplier(),
                        hooks.eventListenerSupplier(),
                        hooks.projectedRollDiceActionAvailableSupplier(),
                        hooks.projectedEndTurnActionAvailableSupplier()
                )
        );

        GameDesktopAssemblyFactory.GameDesktopAssembly desktopAssembly = gameDesktopAssemblyFactory.create(
                config.runtime(),
                config.restoredSessionState(),
                config.sessionId(),
                config.desktopControls().buttons(),
                config.turnEngine(),
                presentationCoordinator.createRuntimeAssemblyHooks(shellDependencies),
                sessionCoordinator.createSessionBridgeHooks(shellDependencies),
                presentationCoordinator.createUiSessionControls(shellDependencies),
                presentationCoordinator.createPresentationHooks(shellDependencies),
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
                config.botTurnScheduler(),
                config.debugPerformanceStats(),
                config.desktopControls().allButtons()
        );
        GameFrameCoordinator.FrameHooks frameHooks = presentationCoordinator.createFrameHooks(shellDependencies);
        LocalHostedGameLoopCoordinator localHostedGameLoopCoordinator = new LocalHostedGameLoopCoordinator(
                () -> gameFrameCoordinator.advancePresentationFrame(frameHooks),
                config.botTurnDriver(),
                desktopAssembly.gameBotTurnHooks(),
                config.debugPerformanceStats()
        );

        return new GameDesktopHostContext(
                sessionCoordinator,
                presentationCoordinator,
                shellDependencies,
                desktopAssembly.board(),
                desktopAssembly.players(),
                desktopAssembly.dices(),
                desktopAssembly.animations(),
                desktopAssembly.debtController(),
                desktopAssembly.debugController(),
                desktopAssembly.sessionCommandPort(),
                desktopAssembly.sessionPresentationStatePort(),
                desktopAssembly.sessionPaymentPort(),
                desktopAssembly.internalCommandPort(),
                desktopAssembly.debtActionDispatcher(),
                desktopAssembly.gameControlLayout(),
                desktopAssembly.gamePrimaryTurnControls(),
                desktopAssembly.gameSessionQueries(),
                desktopAssembly.gameTurnFlowCoordinator(),
                desktopAssembly.gameUiController(),
                desktopAssembly.gameBotTurnHooks(),
                gameFrameCoordinator,
                localHostedGameLoopCoordinator
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
            GameDesktopSessionCoordinator sessionCoordinator,
            GameDesktopPresentationCoordinator presentationCoordinator,
            GameDesktopShellDependencies shellDependencies,
            Board board,
            Players players,
            Dices dices,
            Animations animations,
            DebtController debtController,
            DebugController debugController,
            SessionCommandPort sessionCommandPort,
            SessionPresentationStatePort sessionPresentationStatePort,
            SessionPaymentPort sessionPaymentPort,
            SessionCommandPort internalCommandPort,
            DebtActionDispatcher debtActionDispatcher,
            fi.monopoly.presentation.game.desktop.ui.GameControlLayout gameControlLayout,
            GamePrimaryTurnControls gamePrimaryTurnControls,
            GameSessionQueries gameSessionQueries,
            GameTurnFlowCoordinator gameTurnFlowCoordinator,
            GameUiController gameUiController,
            GameBotTurnDriver.Hooks gameBotTurnHooks,
            GameFrameCoordinator gameFrameCoordinator,
            LocalHostedGameLoopCoordinator localHostedGameLoopCoordinator
    ) {
    }

    public interface Hooks {
        static Hooks of(
                Supplier<GameSessionState> sessionStateSupplier,
                Supplier<Players> playersSupplier,
                BooleanSupplier hasActiveTurnSupplier,
                BooleanSupplier isComputerTurnSupplier,
                Supplier<String> turnPlayerNameSupplier,
                Supplier<Board> boardSupplier,
                Supplier<Dices> dicesSupplier,
                Supplier<Animations> animationsSupplier,
                Supplier<DebtController> debtControllerSupplier,
                Supplier<DebtState> debtStateSupplier,
                Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier,
                Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier,
                Supplier<GameSessionQueries> gameSessionQueriesSupplier,
                Supplier<SessionCommandPort> sessionCommandPortSupplier,
                Supplier<SessionPresentationStatePort> sessionPresentationStateSupplier,
                Function<String, GameView> currentGameViewFactory,
                Function<String, PlayerView> currentPlayerViewFactory,
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
                GameDesktopShellDependencies.PaymentRequestHandler paymentRequestHandler,
                Consumer<Boolean> endRoundAction,
                GameDesktopShellDependencies.ScheduleNextComputerAction scheduleNextComputerAction,
                Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction,
                Consumer<String> focusPlayerByIdAction,
                IntSupplier goMoneyAmountSupplier,
                BooleanSupplier retryDebtVisibleSupplier,
                BooleanSupplier declareBankruptcyVisibleSupplier,
                Supplier<MonopolyEventListener> eventListenerSupplier,
                BooleanSupplier projectedRollDiceActionAvailableSupplier,
                BooleanSupplier projectedEndTurnActionAvailableSupplier
        ) {
            return new DefaultHooks(
                    sessionStateSupplier,
                    playersSupplier,
                    hasActiveTurnSupplier,
                    isComputerTurnSupplier,
                    turnPlayerNameSupplier,
                    boardSupplier,
                    dicesSupplier,
                    animationsSupplier,
                    debtControllerSupplier,
                    debtStateSupplier,
                    gameTurnFlowCoordinatorSupplier,
                    gamePrimaryTurnControlsSupplier,
                    gameSessionQueriesSupplier,
                    sessionCommandPortSupplier,
                    sessionPresentationStateSupplier,
                    currentGameViewFactory,
                    currentPlayerViewFactory,
                    refreshLabelsAction,
                    rollDiceAction,
                    setupDefaultGameStateAction,
                    hidePrimaryTurnControlsAction,
                    showRollDiceControlAction,
                    showEndTurnControlAction,
                    updateDebtButtonsAction,
                    syncTransientPresentationStateAction,
                    updateLogTurnContextAction,
                    retryPendingDebtPaymentAction,
                    paymentRequestHandler,
                    endRoundAction,
                    scheduleNextComputerAction,
                    resumeContinuationAction,
                    focusPlayerByIdAction,
                    goMoneyAmountSupplier,
                    retryDebtVisibleSupplier,
                    declareBankruptcyVisibleSupplier,
                    eventListenerSupplier,
                    projectedRollDiceActionAvailableSupplier,
                    projectedEndTurnActionAvailableSupplier
            );
        }

        Supplier<GameSessionState> sessionStateSupplier();

        Supplier<Players> playersSupplier();

        BooleanSupplier hasActiveTurnSupplier();

        BooleanSupplier isComputerTurnSupplier();

        Supplier<String> turnPlayerNameSupplier();

        Supplier<Board> boardSupplier();

        Supplier<Dices> dicesSupplier();

        Supplier<Animations> animationsSupplier();

        Supplier<DebtController> debtControllerSupplier();

        Supplier<DebtState> debtStateSupplier();

        Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier();

        Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier();

        Supplier<GameSessionQueries> gameSessionQueriesSupplier();

        Supplier<SessionCommandPort> sessionCommandPortSupplier();

        Supplier<SessionPresentationStatePort> sessionPresentationStateSupplier();

        Function<String, GameView> currentGameViewFactory();

        Function<String, PlayerView> currentPlayerViewFactory();

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

        Consumer<String> focusPlayerByIdAction();

        IntSupplier goMoneyAmountSupplier();

        BooleanSupplier retryDebtVisibleSupplier();

        BooleanSupplier declareBankruptcyVisibleSupplier();

        Supplier<MonopolyEventListener> eventListenerSupplier();

        BooleanSupplier projectedRollDiceActionAvailableSupplier();

        BooleanSupplier projectedEndTurnActionAvailableSupplier();
    }

    private record DefaultHooks(
            Supplier<GameSessionState> sessionStateSupplier,
            Supplier<Players> playersSupplier,
            BooleanSupplier hasActiveTurnSupplier,
            BooleanSupplier isComputerTurnSupplier,
            Supplier<String> turnPlayerNameSupplier,
            Supplier<Board> boardSupplier,
            Supplier<Dices> dicesSupplier,
            Supplier<Animations> animationsSupplier,
            Supplier<DebtController> debtControllerSupplier,
            Supplier<DebtState> debtStateSupplier,
            Supplier<GameTurnFlowCoordinator> gameTurnFlowCoordinatorSupplier,
            Supplier<GamePrimaryTurnControls> gamePrimaryTurnControlsSupplier,
            Supplier<GameSessionQueries> gameSessionQueriesSupplier,
            Supplier<SessionCommandPort> sessionCommandPortSupplier,
            Supplier<SessionPresentationStatePort> sessionPresentationStateSupplier,
            Function<String, GameView> currentGameViewFactory,
            Function<String, PlayerView> currentPlayerViewFactory,
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
            GameDesktopShellDependencies.PaymentRequestHandler paymentRequestHandler,
            Consumer<Boolean> endRoundAction,
            GameDesktopShellDependencies.ScheduleNextComputerAction scheduleNextComputerAction,
            Consumer<fi.monopoly.domain.session.TurnContinuationState> resumeContinuationAction,
            Consumer<String> focusPlayerByIdAction,
            IntSupplier goMoneyAmountSupplier,
            BooleanSupplier retryDebtVisibleSupplier,
            BooleanSupplier declareBankruptcyVisibleSupplier,
            Supplier<MonopolyEventListener> eventListenerSupplier,
            BooleanSupplier projectedRollDiceActionAvailableSupplier,
            BooleanSupplier projectedEndTurnActionAvailableSupplier
    ) implements Hooks {
    }
}
