package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.turn.TurnContinuationGateway;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentTarget;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.GameDesktopAssemblyFactory;
import fi.monopoly.presentation.game.desktop.GameDesktopControlsFactory;
import fi.monopoly.presentation.game.desktop.GameDesktopLifecycleCoordinator;
import fi.monopoly.presentation.game.desktop.GameDesktopShellCoordinator;
import fi.monopoly.presentation.game.desktop.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.DebugController;
import fi.monopoly.presentation.game.desktop.LocalSessionActions;
import fi.monopoly.presentation.game.desktop.SessionViewFacade;
import fi.monopoly.presentation.game.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GameControlLayout;
import fi.monopoly.presentation.game.desktop.ui.GameFrameCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarPresenter;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarStateFactory;
import fi.monopoly.presentation.game.desktop.ui.GameUiController;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.text.UiTexts;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import processing.event.Event;

import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
/**
 * Main desktop gameplay composition root.
 *
 * <p>{@code Game} still owns the live Processing-era runtime objects, rendering loop, and many UI
 * controls, but it now delegates an increasing share of turn, decision, debt, trade, and
 * persistence behavior into separated application and presentation adapters. In practice this is
 * the local shell around the older client runtime while the project moves toward a backend-ready
 * session architecture.</p>
 */
public class Game implements MonopolyEventListener {
    private static final String LOCAL_SESSION_ID = "local-session";
    // UI and layout constants
    private static final List<Locale> SUPPORTED_UI_LOCALES = List.of(
            Locale.forLanguageTag("fi"),
            Locale.ENGLISH
    );
    private static final boolean FORCE_DEBT_DEBUG_SCENARIO = false;
    private void setupDebugGameConfigs(MonopolyRuntime runtime, Board board, Players players) {
        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(runtime, text("game.player.default1"), Color.MEDIUMPURPLE, spot, ComputerPlayerProfile.STRONG));
        players.addPlayer(new Player(runtime, text("game.player.default2"), Color.PINK, spot, ComputerPlayerProfile.STRONG));
        players.addPlayer(new Player(runtime, text("game.player.default3"), Color.DARKOLIVEGREEN, spot, ComputerPlayerProfile.STRONG));

        //        players.addPlayer(new Player("Neljäs", Color.TURQUOISE, spot));
        //        players.addPlayer(new Player("Viides", Color.MEDIUMBLUE, spot));
        //        players.addPlayer(new Player("Kuudes", Color.MEDIUMSPRINGGREEN, spot));

        //        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B1));
        //        players.getTurn().buyProperty(PropertyFactory.getProperty(SpotType.B2));
        //        if (!FORCE_DEBT_DEBUG_SCENARIO) {
        //            players.giveRandomDeeds(board);
        //        }
    }

    // Core services
    private final MonopolyRuntime runtime;
    private final TurnEngine turnEngine = new TurnEngine();
    private final GameSessionStateCoordinator gameSessionStateCoordinator = new GameSessionStateCoordinator();
    private final GameBotTurnControlCoordinator gameBotTurnControlCoordinator = new GameBotTurnControlCoordinator();
    private final GameDesktopAssemblyFactory gameDesktopAssemblyFactory = new GameDesktopAssemblyFactory();
    private final GameDesktopShellCoordinator gameDesktopShellCoordinator;
    private final GameDesktopLifecycleCoordinator gameDesktopLifecycleCoordinator = new GameDesktopLifecycleCoordinator();
    private final GameDesktopControlsFactory gameDesktopControlsFactory = new GameDesktopControlsFactory();
    private final GameDesktopShellDependencies shellDependencies;
    private final SessionApplicationService sessionApplicationService;
    private final DebtActionDispatcher debtActionDispatcher;
    private DebtController debtController;
    private DebugController debugController;

    // UI controls
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugGodModeButton;
    private final MonopolyButton pauseButton;
    private final MonopolyButton tradeButton;
    private final MonopolyButton saveButton;
    private final MonopolyButton loadButton;
    private final MonopolyButton botSpeedButton;
    private final MonopolyButton languageButton;
    private final List<MonopolyButton> desktopButtons;

    // Mutable game state
    private Players players;
    private Dices dices;
    private Animations animations;
    private int goMoneyAmount = 200;
    private Board board;
    private final GameSessionState sessionState = new GameSessionState();
    private final BotTurnScheduler botTurnScheduler = new BotTurnScheduler();
    private final GameBotTurnDriver botTurnDriver = new GameBotTurnDriver(botTurnScheduler);
    private final DebugPerformanceStats debugPerformanceStats = new DebugPerformanceStats();
    private final SessionViewFacade sessionViewFacade;
    private GameControlLayout gameControlLayout;
    private GamePrimaryTurnControls gamePrimaryTurnControls;
    private GameSessionQueries gameSessionQueries;
    private final GameUiController gameUiController;
    private GameTurnFlowCoordinator gameTurnFlowCoordinator;
    private GameBotTurnDriver.Hooks gameBotTurnHooks;
    private GameFrameCoordinator gameFrameCoordinator;
    public Game(MonopolyRuntime runtime) {
        this(runtime, null, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState) {
        this(runtime, restoredSessionState, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState, LocalSessionActions localSessionActions) {
        this.runtime = runtime;
        this.gameDesktopShellCoordinator = new GameDesktopShellCoordinator(
                runtime,
                LOCAL_SESSION_ID,
                SUPPORTED_UI_LOCALES,
                localSessionActions,
                gameSessionStateCoordinator,
                gameBotTurnControlCoordinator
        );
        GameDesktopControlsFactory.GameDesktopControls desktopControls = gameDesktopControlsFactory.create(runtime);
        this.endRoundButton = desktopControls.buttons().endRoundButton();
        this.retryDebtButton = desktopControls.buttons().retryDebtButton();
        this.declareBankruptcyButton = desktopControls.buttons().declareBankruptcyButton();
        this.debugGodModeButton = desktopControls.buttons().debugGodModeButton();
        this.pauseButton = desktopControls.buttons().pauseButton();
        this.tradeButton = desktopControls.buttons().tradeButton();
        this.saveButton = desktopControls.buttons().saveButton();
        this.loadButton = desktopControls.buttons().loadButton();
        this.botSpeedButton = desktopControls.buttons().botSpeedButton();
        this.languageButton = desktopControls.buttons().languageButton();
        this.desktopButtons = desktopControls.allButtons();
        this.shellDependencies = new GameDesktopShellDependencies(
                () -> sessionState,
                this::players,
                this::currentTurnPlayer,
                this::playerById,
                this::getBoard,
                this::dices,
                this::animations,
                this::debtController,
                this::currentDebtState,
                this::gameTurnFlowCoordinatorRef,
                this::gamePrimaryTurnControlsRef,
                this::gameSessionQueriesRef,
                this::sessionApplicationServiceRef,
                () -> runtime.popupService(),
                () -> botTurnScheduler,
                this::currentGameView,
                this::currentPlayerView,
                this::refreshLabels,
                this::rollDice,
                (board, players) -> setupDebugGameConfigs(runtime, board, players),
                this::hidePrimaryTurnControls,
                this::showRollDiceControl,
                this::showEndTurnControl,
                this::updateDebtButtons,
                this::syncTransientPresentationState,
                this::updateLogTurnContext,
                this::retryPendingDebtPaymentAction,
                this::handlePaymentRequest,
                this::endRound,
                this::scheduleNextComputerAction,
                this::resumeContinuation,
                this::focusPlayer,
                () -> goMoneyAmount,
                this::retryDebtVisible,
                this::declareBankruptcyVisible,
                this::endRoundVisible,
                this::rollDiceVisible,
                () -> this
        );
        GameDesktopAssemblyFactory.GameDesktopAssembly desktopAssembly = gameDesktopAssemblyFactory.create(
                runtime,
                restoredSessionState,
                LOCAL_SESSION_ID,
                desktopControls.buttons(),
                turnEngine,
                gameDesktopShellCoordinator.createRuntimeAssemblyHooks(shellDependencies),
                gameDesktopShellCoordinator.createSessionBridgeHooks(shellDependencies),
                gameDesktopShellCoordinator.createPresentationHooks(shellDependencies),
                debugPerformanceStats
        );
        this.board = desktopAssembly.board();
        this.players = desktopAssembly.players();
        this.dices = desktopAssembly.dices();
        this.animations = desktopAssembly.animations();
        this.debtController = desktopAssembly.debtController();
        this.debugController = desktopAssembly.debugController();
        this.sessionApplicationService = desktopAssembly.sessionApplicationService();
        this.debtActionDispatcher = desktopAssembly.debtActionDispatcher();
        this.sessionViewFacade = gameDesktopShellCoordinator.createSessionViewFacade(shellDependencies);
        gameDesktopShellCoordinator.applyRestoredSessionState(shellDependencies, restoredSessionState);
        this.gameControlLayout = desktopAssembly.gameControlLayout();
        this.gamePrimaryTurnControls = desktopAssembly.gamePrimaryTurnControls();
        this.gameSessionQueries = desktopAssembly.gameSessionQueries();
        this.gameTurnFlowCoordinator = desktopAssembly.gameTurnFlowCoordinator();
        this.gameUiController = desktopAssembly.gameUiController();
        this.gameBotTurnHooks = desktopAssembly.gameBotTurnHooks();
        this.gameFrameCoordinator = new GameFrameCoordinator(
                runtime,
                gameControlLayout,
                new GameSidebarPresenter(runtime),
                desktopAssembly.gamePresentationSupport(),
                gamePrimaryTurnControls,
                new GameSidebarStateFactory(),
                gameSessionStateCoordinator,
                botTurnDriver,
                botTurnScheduler,
                debugPerformanceStats,
                desktopButtons
        );
        gameDesktopShellCoordinator.initializeSessionPresentation(shellDependencies, restoredSessionState);
        setupButtonActions();

        if (FORCE_DEBT_DEBUG_SCENARIO) {
            debugController.initializeDebtDebugScenario();
        }
    }

    Board getBoard() {
        return board;
    }

    SessionState projectedSessionState() {
        return sessionApplicationService.currentState();
    }

    public SessionState sessionStateForPersistence() {
        return sessionApplicationService.currentState();
    }

    public void showPersistenceNotice(String notice) {
        gameDesktopShellCoordinator.showPersistenceNotice(sessionState, notice);
    }

    void refreshProjectedSessionState() {
        sessionApplicationService.handle(new RefreshSessionViewCommand(LOCAL_SESSION_ID));
    }

    DebtController debtController() {
        return debtController;
    }

    private void onDebtStateChanged() {
        gameDesktopShellCoordinator.onDebtStateChanged(shellDependencies);
    }

    private void setupButtonActions() {
        gameUiController.bindButtonActions();
    }

    Players players() {
        return players;
    }

    private Player currentTurnPlayer() {
        return players != null ? players.getTurn() : null;
    }

    Dices dices() {
        return dices;
    }

    Animations animations() {
        return animations;
    }

    private DebtState currentDebtState() {
        return debtController != null ? debtController.debtState() : null;
    }

    private GameTurnFlowCoordinator gameTurnFlowCoordinatorRef() {
        return gameTurnFlowCoordinator;
    }

    private GamePrimaryTurnControls gamePrimaryTurnControlsRef() {
        return gamePrimaryTurnControls;
    }

    private GameSessionQueries gameSessionQueriesRef() {
        return gameSessionQueries;
    }

    private SessionApplicationService sessionApplicationServiceRef() {
        return sessionApplicationService;
    }

    private GameView currentGameView() {
        return createGameView(currentTurnPlayer());
    }

    private PlayerView currentPlayerView() {
        return createPlayerView(currentTurnPlayer());
    }

    private void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        if (continuationState != null && gameTurnFlowCoordinator != null) {
            gameTurnFlowCoordinator.resumeContinuation(continuationState);
        }
    }

    private void focusPlayer(Player player) {
        if (players != null && player != null) {
            players.focusPlayer(player);
        }
    }

    private boolean retryDebtVisible() {
        return retryDebtButton.isVisible();
    }

    private boolean declareBankruptcyVisible() {
        return declareBankruptcyButton.isVisible();
    }

    private boolean endRoundVisible() {
        return endRoundButton.isVisible();
    }

    private boolean rollDiceVisible() {
        return dices != null && dices.isVisible();
    }

    public void draw() {
        gameFrameCoordinator.drawFrame(createFrameHooks());
    }

    private LayoutMetrics updateFrameLayoutMetrics() {
        return gameFrameCoordinator.updateFrameLayoutMetrics();
    }

    private boolean isDebtSidebarMode() {
        return debtController.debtState() != null;
    }

    private void rollDice() {
        gameTurnFlowCoordinator.rollDice();
    }

    private void runComputerPlayerStep() {
        gameFrameCoordinator.runComputerPlayerStep(gameBotTurnHooks);
    }

    private void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        gameFrameCoordinator.applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning, createFrameHooks());
    }

    private void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        botTurnScheduler.schedule(
                delayKind,
                now,
                sessionState.botSpeedMode(),
                players.getPlayers().stream().allMatch(Player::isComputerControlled)
        );
    }

    private void endRound(boolean switchTurns) {
        gameTurnFlowCoordinator.endRound(switchTurns);
    }

    public boolean onEvent(Event event) {
        updateLogTurnContext();
        return gameUiController.handleEvent(event);
    }

    private void handlePaymentRequest(PaymentRequest request, fi.monopoly.domain.session.TurnContinuationState continuationState, CallbackAction onResolved) {
        updateLogTurnContext();
        sessionApplicationService.handlePaymentRequest(request, continuationState, onResolved);
    }

    private void retryPendingDebtPaymentAction() {
        if (debtActionDispatcher != null) {
            debtActionDispatcher.payDebt();
            return;
        }
        debtController.retryPendingDebtPayment();
    }

    private LayoutMetrics getLayoutMetrics() {
        return gameFrameCoordinator.getLayoutMetrics();
    }

    private void updateSidebarControlPositions() {
        gameFrameCoordinator.updateSidebarControlPositions();
    }

    private void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        gameFrameCoordinator.updateSidebarControlPositions(layoutMetrics);
    }

    private float getSidebarHistoryHeight() {
        return gameFrameCoordinator.getSidebarHistoryHeight();
    }

    private float getSidebarHistoryPanelY() {
        return gameFrameCoordinator.getSidebarHistoryPanelY();
    }

    private float getSidebarReservedTop() {
        return gameFrameCoordinator.getSidebarReservedTop();
    }

    private GameSidebarPresenter.SidebarState createSidebarState() {
        return gameFrameCoordinator.createSidebarState(createFrameHooks());
    }

    private float getSidebarContentTop() {
        return gameFrameCoordinator.getSidebarContentTop(createFrameHooks());
    }

    private void updateDebtButtons() {
        gameFrameCoordinator.updateDebtButtons(
                debtController.debtState(),
                sessionApplicationService != null ? sessionApplicationService.currentState() : null
        );
    }

    private void updateDebugButtons() {
        gameFrameCoordinator.updatePersistentButtons(sessionState.gameOver());
    }

    private void refreshButtonInteractivityState() {
        gameFrameCoordinator.refreshButtonInteractivityState();
    }

    private void refreshLabels() {
        gameFrameCoordinator.refreshLabels(sessionState.paused(), sessionState.botSpeedMode());
    }

    private void togglePause() {
        gameDesktopShellCoordinator.togglePause(shellDependencies);
    }

    private void cycleBotSpeedMode() {
        gameDesktopShellCoordinator.cycleBotSpeedMode(shellDependencies);
    }

    private void switchLanguage(Locale locale) {
        gameDesktopShellCoordinator.switchLanguage(locale);
    }

    private void debugResetTurnState() {
        gameDesktopShellCoordinator.debugResetTurnState(shellDependencies);
    }

    private void restoreNormalTurnControls() {
        gameDesktopShellCoordinator.restoreNormalTurnControls(shellDependencies);
    }

    private void showRollDiceControl() {
        gamePrimaryTurnControls.showRollDiceControl();
    }

    private void showEndTurnControl() {
        gamePrimaryTurnControls.showEndTurnControl();
    }

    private void hidePrimaryTurnControls() {
        gamePrimaryTurnControls.hide();
    }

    private void declareWinner(Player winningPlayer) {
        gameDesktopShellCoordinator.declareWinner(shellDependencies, winningPlayer);
    }

    private void updateLogTurnContext() {
        gameFrameCoordinator.updateLogTurnContext(sessionState.gameOver(), sessionState.winner(), players != null ? players.getTurn() : null);
    }

    private void syncTransientPresentationState() {
        gameFrameCoordinator.syncTransientPresentationState(this::restoreBotTurnControlsIfNeeded);
    }

    private Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        return players.getPlayers().stream()
                .filter(player -> ("player-" + player.getId()).equals(playerId))
                .findFirst()
                .orElse(null);
    }

    private void enforcePrimaryTurnControlInvariant() {
        gameFrameCoordinator.enforcePrimaryTurnControlInvariant(debtController.debtState() != null);
    }

    private GameFrameCoordinator.FrameHooks createFrameHooks() {
        return gameDesktopShellCoordinator.createFrameHooks(shellDependencies, gameBotTurnHooks);
    }

    GameView createGameView(Player currentPlayer) {
        long snapshotStart = System.nanoTime();
        GameView view = sessionViewFacade.createGameView(currentPlayer);
        debugPerformanceStats.recordGameViewBuild(System.nanoTime() - snapshotStart);
        return view;
    }

    public List<String> debugPerformanceLines(float fps) {
        return debugPerformanceStats.overlayLines(fps, MonopolyApp.getColoredImageCopies());
    }

    PlayerView createPlayerView(Player player) {
        return sessionViewFacade.createPlayerView(player);
    }

    private boolean isRollDiceActionAvailable(Player currentPlayer) {
        return gameDesktopShellCoordinator.isRollDiceActionAvailable(shellDependencies, currentPlayer);
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        return gameDesktopShellCoordinator.isEndTurnActionAvailable(shellDependencies, currentPlayer);
    }

    private boolean isProjectedRollDiceActionAvailable() {
        return isRollDiceActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean isProjectedEndTurnActionAvailable() {
        return isEndTurnActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean restoreBotTurnControlsIfNeeded() {
        return gameDesktopShellCoordinator.restoreBotTurnControlsIfNeeded(shellDependencies);
    }

    private GameBotTurnControlCoordinator.Hooks createBotTurnControlHooks() {
        return gameDesktopShellCoordinator.createBotTurnControlHooks(shellDependencies);
    }

    public void dispose() {
        gameDesktopLifecycleCoordinator.dispose(
                runtime,
                this,
                players,
                dices,
                desktopButtons
        );
    }

}
