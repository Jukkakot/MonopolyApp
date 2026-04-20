package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.assembly.GameDesktopHostFactory;
import fi.monopoly.presentation.game.desktop.runtime.GameDesktopLifecycleCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.desktop.session.LocalSessionActions;
import fi.monopoly.presentation.game.desktop.ui.GameDesktopControlsFactory;
import fi.monopoly.presentation.game.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.bot.GameBotTurnControlCoordinator;
import fi.monopoly.presentation.game.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.desktop.ui.GameDesktopPresentationHost;
import fi.monopoly.presentation.game.desktop.ui.GamePrimaryTurnControls;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarPresenter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
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
    void setupDefaultGameState(MonopolyRuntime runtime, Board board, Players players) {
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
    private final GameDesktopHostFactory gameDesktopHostFactory = new GameDesktopHostFactory();
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
    private final GameDesktopPresentationHost presentationHost;
    private GameTurnFlowCoordinator gameTurnFlowCoordinator;
    public Game(MonopolyRuntime runtime) {
        this(runtime, null, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState) {
        this(runtime, restoredSessionState, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState, LocalSessionActions localSessionActions) {
        this.runtime = runtime;
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
        GameDesktopHostFactory.GameDesktopHostContext hostContext = gameDesktopHostFactory.create(
                new GameDesktopHostFactory.Config(
                        runtime,
                        restoredSessionState,
                        localSessionActions,
                        LOCAL_SESSION_ID,
                        SUPPORTED_UI_LOCALES,
                        turnEngine,
                        gameSessionStateCoordinator,
                        gameBotTurnControlCoordinator,
                        botTurnDriver,
                        botTurnScheduler,
                        debugPerformanceStats,
                        desktopControls
                ),
                new GameDesktopHostHooksAdapter(runtime, this)
        );
        this.gameDesktopShellCoordinator = hostContext.shellCoordinator();
        this.shellDependencies = hostContext.shellDependencies();
        this.board = hostContext.board();
        this.players = hostContext.players();
        this.dices = hostContext.dices();
        this.animations = hostContext.animations();
        this.debtController = hostContext.debtController();
        this.debugController = hostContext.debugController();
        this.sessionApplicationService = hostContext.sessionApplicationService();
        this.debtActionDispatcher = hostContext.debtActionDispatcher();
        this.gameTurnFlowCoordinator = hostContext.gameTurnFlowCoordinator();
        this.presentationHost = new GameDesktopPresentationHost(
                runtime,
                gameDesktopShellCoordinator,
                shellDependencies,
                debugPerformanceStats,
                () -> sessionState,
                this::currentTurnPlayer,
                hostContext.gamePrimaryTurnControls(),
                hostContext.gameSessionQueries(),
                hostContext.gameUiController(),
                hostContext.gameBotTurnHooks(),
                hostContext.gameFrameCoordinator()
        );
        gameDesktopShellCoordinator.applyRestoredSessionState(shellDependencies, restoredSessionState);
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

    GameSessionState sessionStateRef() {
        return sessionState;
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
        presentationHost.bindButtonActions();
    }

    Players players() {
        return players;
    }

    Player currentTurnPlayer() {
        return players != null ? players.getTurn() : null;
    }

    Dices dices() {
        return dices;
    }

    Animations animations() {
        return animations;
    }

    DebtState currentDebtState() {
        return debtController != null ? debtController.debtState() : null;
    }

    GameTurnFlowCoordinator gameTurnFlowCoordinatorRef() {
        return gameTurnFlowCoordinator;
    }

    GamePrimaryTurnControls gamePrimaryTurnControlsRef() {
        return presentationHost.primaryTurnControls();
    }

    GameSessionQueries gameSessionQueriesRef() {
        return presentationHost.gameSessionQueries();
    }

    SessionApplicationService sessionApplicationServiceRef() {
        return sessionApplicationService;
    }

    GameView currentGameView() {
        return createGameView(currentTurnPlayer());
    }

    PlayerView currentPlayerView() {
        return createPlayerView(currentTurnPlayer());
    }

    void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
        if (continuationState != null && gameTurnFlowCoordinator != null) {
            gameTurnFlowCoordinator.resumeContinuation(continuationState);
        }
    }

    void focusPlayer(Player player) {
        if (players != null && player != null) {
            players.focusPlayer(player);
        }
    }

    boolean retryDebtVisible() {
        return retryDebtButton.isVisible();
    }

    boolean declareBankruptcyVisible() {
        return declareBankruptcyButton.isVisible();
    }

    boolean endRoundVisible() {
        return endRoundButton.isVisible();
    }

    boolean rollDiceVisible() {
        return dices != null && dices.isVisible();
    }

    int goMoneyAmountRef() {
        return goMoneyAmount;
    }

    public void draw() {
        presentationHost.draw();
    }

    private LayoutMetrics updateFrameLayoutMetrics() {
        return presentationHost.updateFrameLayoutMetrics();
    }

    void rollDice() {
        gameTurnFlowCoordinator.rollDice();
    }

    void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        botTurnScheduler.schedule(
                delayKind,
                now,
                sessionState.botSpeedMode(),
                players.getPlayers().stream().allMatch(Player::isComputerControlled)
        );
    }

    void endRound(boolean switchTurns) {
        gameTurnFlowCoordinator.endRound(switchTurns);
    }

    public boolean onEvent(Event event) {
        return presentationHost.handleEvent(event);
    }

    void handlePaymentRequest(PaymentRequest request, fi.monopoly.domain.session.TurnContinuationState continuationState, CallbackAction onResolved) {
        updateLogTurnContext();
        sessionApplicationService.handlePaymentRequest(request, continuationState, onResolved);
    }

    private LayoutMetrics getLayoutMetrics() {
        return presentationHost.getLayoutMetrics();
    }

    private void updateSidebarControlPositions() {
        presentationHost.updateSidebarControlPositions();
    }

    private void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        presentationHost.updateSidebarControlPositions(layoutMetrics);
    }

    private float getSidebarHistoryHeight() {
        return presentationHost.getSidebarHistoryHeight();
    }

    private float getSidebarHistoryPanelY() {
        return presentationHost.getSidebarHistoryPanelY();
    }

    private float getSidebarReservedTop() {
        return presentationHost.getSidebarReservedTop();
    }

    private GameSidebarPresenter.SidebarState createSidebarState() {
        return presentationHost.createSidebarState();
    }

    private float getSidebarContentTop() {
        return presentationHost.getSidebarContentTop();
    }

    void retryPendingDebtPaymentAction() {
        if (debtActionDispatcher != null) {
            debtActionDispatcher.payDebt();
            return;
        }
        debtController.retryPendingDebtPayment();
    }

    void updateDebtButtons() {
        presentationHost.updateDebtButtons(sessionApplicationService != null ? sessionApplicationService.currentState() : null);
    }

    private void updateDebugButtons() {
        presentationHost.updateDebugButtons();
    }

    private void refreshButtonInteractivityState() {
        presentationHost.refreshButtonInteractivityState();
    }

    void refreshLabels() {
        presentationHost.refreshLabels();
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

    void showRollDiceControl() {
        presentationHost.showRollDiceControl();
    }

    void showEndTurnControl() {
        presentationHost.showEndTurnControl();
    }

    void hidePrimaryTurnControls() {
        presentationHost.hidePrimaryTurnControls();
    }

    private void declareWinner(Player winningPlayer) {
        gameDesktopShellCoordinator.declareWinner(shellDependencies, winningPlayer);
    }

    void updateLogTurnContext() {
        presentationHost.updateLogTurnContext();
    }

    void syncTransientPresentationState() {
        presentationHost.syncTransientPresentationState();
    }

    private void runComputerPlayerStep() {
        presentationHost.runComputerPlayerStep();
    }

    private void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        presentationHost.applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning);
    }

    Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        return players.getPlayers().stream()
                .filter(player -> ("player-" + player.getId()).equals(playerId))
                .findFirst()
                .orElse(null);
    }

    GameView createGameView(Player currentPlayer) {
        return presentationHost.createGameView(currentPlayer);
    }

    public List<String> debugPerformanceLines(float fps) {
        return presentationHost.debugPerformanceLines(fps);
    }

    PlayerView createPlayerView(Player player) {
        return presentationHost.createPlayerView(player);
    }

    private void enforcePrimaryTurnControlInvariant() {
        presentationHost.enforcePrimaryTurnControlInvariant();
    }

    private boolean isRollDiceActionAvailable(Player currentPlayer) {
        return gameDesktopShellCoordinator.isRollDiceActionAvailable(shellDependencies, currentPlayer);
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        return gameDesktopShellCoordinator.isEndTurnActionAvailable(shellDependencies, currentPlayer);
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
