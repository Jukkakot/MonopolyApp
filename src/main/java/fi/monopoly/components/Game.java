package fi.monopoly.components;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.client.session.SessionPaymentPort;
import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.client.session.ForwardingSessionCommandPort;
import fi.monopoly.client.session.SessionCommandPort;
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
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.session.local.LocalHostedGameLoopCoordinator;
import fi.monopoly.presentation.game.desktop.assembly.GameDesktopBootstrapFactory;
import fi.monopoly.presentation.game.desktop.assembly.GameDesktopHostFactory;
import fi.monopoly.presentation.game.desktop.runtime.GameDesktopLifecycleCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopPresentationCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopSessionCoordinator;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnControlCoordinator;
import fi.monopoly.host.bot.GameBotTurnDriver;
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
    void setupDefaultGameState(MonopolyRuntime runtime, Board board, Players players) {
        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(runtime, text("game.player.default1"), Color.MEDIUMPURPLE, spot, ComputerPlayerProfile.STRONG));
        players.addPlayer(new Player(runtime, text("game.player.default2"), Color.PINK, spot, ComputerPlayerProfile.STRONG));
        players.addPlayer(new Player(runtime, text("game.player.default3"), Color.DARKOLIVEGREEN, spot, ComputerPlayerProfile.STRONG));
    }

    // Core services
    private final MonopolyRuntime runtime;
    private final TurnEngine turnEngine = new TurnEngine();
    private final GameSessionStateCoordinator gameSessionStateCoordinator = new GameSessionStateCoordinator();
    private final GameBotTurnControlCoordinator gameBotTurnControlCoordinator = new GameBotTurnControlCoordinator();
    private final GameDesktopBootstrapFactory gameDesktopBootstrapFactory = new GameDesktopBootstrapFactory();
    private final GameDesktopSessionCoordinator gameDesktopSessionCoordinator;
    private final GameDesktopPresentationCoordinator gameDesktopPresentationCoordinator;
    private final GameDesktopLifecycleCoordinator gameDesktopLifecycleCoordinator = new GameDesktopLifecycleCoordinator();
    private final GameDesktopShellDependencies shellDependencies;
    private final SessionCommandPort sessionCommandPort;
    private final SessionPresentationStatePort sessionPresentationState;
    private final SessionCommandPort internalCommandPort;
    private final SessionPaymentPort sessionPaymentPort;
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
    private final BotTurnScheduler botTurnScheduler = new BotTurnScheduler(DesktopClientSettings::skipAnimations);
    private final GameBotTurnDriver botTurnDriver = new GameBotTurnDriver(botTurnScheduler);
    private final DebugPerformanceStats debugPerformanceStats = new DebugPerformanceStats();
    private final GameDesktopPresentationHost presentationHost;
    private final LocalHostedGameLoopCoordinator hostedGameLoopCoordinator;
    private final TestFacade testFacade = new TestFacade();
    private GameTurnFlowCoordinator gameTurnFlowCoordinator;
    public Game(MonopolyRuntime runtime) {
        this(runtime, null, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState) {
        this(runtime, restoredSessionState, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState, LocalSessionActions localSessionActions) {
        this.runtime = runtime;
        GameDesktopBootstrapFactory.GameDesktopBootstrap bootstrap = gameDesktopBootstrapFactory.create(
                new GameDesktopBootstrapFactory.Config(
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
                        () -> sessionState
                ),
                GameDesktopHostFactory.Hooks.of(
                        this::sessionStateRef,
                        this::players,
                        () -> players != null && players.getTurn() != null,
                        () -> players != null && players.getTurn() != null && players.getTurn().isComputerControlled(),
                        () -> players != null && players.getTurn() != null ? players.getTurn().getName() : null,
                        this::getBoard,
                        this::dices,
                        this::animations,
                        this::debtController,
                        this::currentDebtState,
                        this::gameTurnFlowCoordinatorRef,
                        this::gamePrimaryTurnControlsRef,
                        this::gameSessionQueriesRef,
                        this::sessionCommandPortRef,
                        this::sessionPresentationStateRef,
                        this::createGameViewById,
                        this::createPlayerViewById,
                        this::refreshLabels,
                        this::rollDice,
                        (board, players) -> setupDefaultGameState(runtime, board, players),
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
                        this::focusPlayerById,
                        this::goMoneyAmountRef,
                        this::retryDebtVisible,
                        this::declareBankruptcyVisible,
                        () -> this,
                        () -> isRollDiceActionAvailable(),
                        () -> isEndTurnActionAvailable()
                )
        );
        var desktopControls = bootstrap.desktopControls();
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
        GameDesktopHostFactory.GameDesktopHostContext hostContext = bootstrap.hostContext();
        this.gameDesktopSessionCoordinator = hostContext.sessionCoordinator();
        this.gameDesktopPresentationCoordinator = hostContext.presentationCoordinator();
        this.shellDependencies = hostContext.shellDependencies();
        this.board = hostContext.board();
        this.players = hostContext.players();
        this.dices = hostContext.dices();
        this.animations = hostContext.animations();
        this.debtController = hostContext.debtController();
        this.debugController = hostContext.debugController();
        this.sessionCommandPort = hostContext.sessionCommandPort();
        this.sessionPresentationState = hostContext.sessionPresentationStatePort();
        this.internalCommandPort = hostContext.internalCommandPort();
        this.sessionPaymentPort = hostContext.sessionPaymentPort();
        this.debtActionDispatcher = hostContext.debtActionDispatcher();
        this.gameTurnFlowCoordinator = hostContext.gameTurnFlowCoordinator();
        this.presentationHost = bootstrap.presentationHost();
        this.hostedGameLoopCoordinator = hostContext.localHostedGameLoopCoordinator();
        this.presentationHost.bindButtonActions();
        gameDesktopSessionCoordinator.applyRestoredSessionState(shellDependencies, restoredSessionState);
        gameDesktopSessionCoordinator.initializeSessionPresentation(shellDependencies, restoredSessionState);

    }

    private Board getBoard() {
        return board;
    }

    private SessionState projectedSessionState() {
        return sessionCommandPort.currentState();
    }

    private GameSessionState sessionStateRef() {
        return sessionState;
    }

    public SessionState sessionStateForPersistence() {
        return sessionCommandPort.currentState();
    }

    public fi.monopoly.application.result.CommandResult submitCommand(fi.monopoly.application.command.SessionCommand command) {
        return internalCommandPort.handle(command);
    }

    public void showPersistenceNotice(String notice) {
        gameDesktopSessionCoordinator.showPersistenceNotice(sessionState, notice);
    }

    private DebtController debtController() {
        return debtController;
    }

    private void onDebtStateChanged() {
        gameDesktopPresentationCoordinator.onDebtStateChanged(shellDependencies);
    }

    private Players players() {
        return players;
    }

    private Player currentTurnPlayer() {
        return players != null ? players.getTurn() : null;
    }

    private Dices dices() {
        return dices;
    }

    private Animations animations() {
        return animations;
    }

    DebtState currentDebtState() {
        return debtController != null ? debtController.debtState() : null;
    }

    private GameTurnFlowCoordinator gameTurnFlowCoordinatorRef() {
        return gameTurnFlowCoordinator;
    }

    private GamePrimaryTurnControls gamePrimaryTurnControlsRef() {
        return presentationHost.primaryTurnControls();
    }

    private GameSessionQueries gameSessionQueriesRef() {
        return presentationHost.gameSessionQueries();
    }

    private SessionCommandPort sessionCommandPortRef() {
        return sessionCommandPort;
    }

    private SessionPresentationStatePort sessionPresentationStateRef() {
        return sessionPresentationState;
    }

    private void resumeContinuation(TurnContinuationState continuationState) {
        if (continuationState != null && gameTurnFlowCoordinator != null) {
            gameTurnFlowCoordinator.resumeContinuation(continuationState);
        }
    }

    private void focusPlayerById(String playerId) {
        if (players == null || playerId == null) return;
        players.getPlayers().stream()
                .filter(p -> playerId.equals("player-" + p.getId()))
                .findFirst()
                .ifPresent(player -> {
                    if (player.getSpot() != null) {
                        player.setCoords(player.getSpot().getTokenCoords(player));
                    }
                    players.focusPlayer(player);
                });
    }

    private boolean retryDebtVisible() {
        return retryDebtButton.isVisible();
    }

    private boolean declareBankruptcyVisible() {
        return declareBankruptcyButton.isVisible();
    }

    private int goMoneyAmountRef() {
        return goMoneyAmount;
    }

    public void draw() {
        presentationHost.render();
    }

    public void advanceHostedFrame() {
        hostedGameLoopCoordinator.advanceFrame();
    }

    public void setExternalCommandDelegate(SessionCommandPort delegate) {
        if (sessionCommandPort instanceof ForwardingSessionCommandPort proxy) {
            proxy.setCommandDelegate(delegate);
        }
    }

    private LayoutMetrics updateFrameLayoutMetrics() {
        return presentationHost.updateFrameLayoutMetrics();
    }

    private void rollDice() {
        gameTurnFlowCoordinator.rollDice();
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
        return presentationHost.handleEvent(event);
    }

    public TestFacade testFacade() {
        return testFacade;
    }

    private void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        updateLogTurnContext();
        sessionPaymentPort.handlePaymentRequest(request, continuationState, onResolved);
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

    private void retryPendingDebtPaymentAction() {
        if (debtActionDispatcher != null) {
            debtActionDispatcher.payDebt();
            return;
        }
        debtController.retryPendingDebtPayment();
    }

    private void updateDebtButtons() {
        presentationHost.updateDebtButtons(sessionCommandPort.currentState());
    }

    private void updateDebugButtons() {
        presentationHost.updateDebugButtons();
    }

    private void refreshButtonInteractivityState() {
        presentationHost.refreshButtonInteractivityState();
    }

    private void refreshLabels() {
        presentationHost.refreshLabels();
    }

    private void togglePause() {
        gameDesktopPresentationCoordinator.togglePause(shellDependencies);
    }

    private void cycleBotSpeedMode() {
        gameDesktopPresentationCoordinator.cycleBotSpeedMode(shellDependencies);
    }

    private void switchLanguage(Locale locale) {
        gameDesktopPresentationCoordinator.switchLanguage(locale);
    }

    private void debugResetTurnState() {
        gameDesktopPresentationCoordinator.debugResetTurnState(shellDependencies);
    }

    private void restoreNormalTurnControls() {
        gameDesktopPresentationCoordinator.restoreNormalTurnControls(shellDependencies);
    }

    private void showRollDiceControl() {
        presentationHost.showRollDiceControl();
    }

    private void showEndTurnControl() {
        presentationHost.showEndTurnControl();
    }

    private void hidePrimaryTurnControls() {
        presentationHost.hidePrimaryTurnControls();
    }

    private void updateLogTurnContext() {
        presentationHost.updateLogTurnContext();
    }

    private void syncTransientPresentationState() {
        presentationHost.syncTransientPresentationState();
    }

    /**
     * Compatibility hook still used by reflection-based bot/smoke tests.
     *
     * <p>The real architecture now routes bot progression through the host-owned local game loop,
     * but these tests still probe the old host surface directly while the transition is ongoing.</p>
     */
    private void runComputerPlayerStep() {
        hostedGameLoopCoordinator.runBotStep();
    }

    private void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        presentationHost.applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning);
    }

    private GameView createGameView(Player currentPlayer) {
        return presentationHost.createGameView(currentPlayer);
    }

    private GameView createGameViewById(String playerId) {
        if (players == null) return null;
        Player p = players.getPlayers().stream()
                .filter(player -> playerId.equals("player-" + player.getId()))
                .findFirst().orElse(null);
        return p != null ? createGameView(p) : null;
    }

    public List<String> debugPerformanceLines(float fps) {
        return presentationHost.debugPerformanceLines(fps);
    }

    private PlayerView createPlayerView(Player player) {
        return presentationHost.createPlayerView(player);
    }

    private PlayerView createPlayerViewById(String playerId) {
        if (players == null) return null;
        Player p = players.getPlayers().stream()
                .filter(player -> playerId.equals("player-" + player.getId()))
                .findFirst().orElse(null);
        return p != null ? createPlayerView(p) : null;
    }

    private void enforcePrimaryTurnControlInvariant() {
        presentationHost.enforcePrimaryTurnControlInvariant();
    }

    private boolean isRollDiceActionAvailable() {
        return gameDesktopPresentationCoordinator.isRollDiceActionAvailable(shellDependencies);
    }

    private boolean isEndTurnActionAvailable() {
        return gameDesktopPresentationCoordinator.isEndTurnActionAvailable(shellDependencies);
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

    /**
     * Centralized test/inspection surface for the compatibility-era desktop host.
     *
     * <p>This keeps test code from depending on scattered private reflection hooks while the
     * project still carries the legacy {@code Game} host. Production code should not route normal
     * behavior through this facade.</p>
     */
    public final class TestFacade {
        public MonopolyButton endRoundButton() {
            return endRoundButton;
        }

        public MonopolyButton retryDebtButton() {
            return retryDebtButton;
        }

        public MonopolyButton declareBankruptcyButton() {
            return declareBankruptcyButton;
        }

        public MonopolyButton debugGodModeButton() {
            return debugGodModeButton;
        }

        public MonopolyButton pauseButton() {
            return pauseButton;
        }

        public MonopolyButton tradeButton() {
            return tradeButton;
        }

        public MonopolyButton saveButton() {
            return saveButton;
        }

        public MonopolyButton loadButton() {
            return loadButton;
        }

        public MonopolyButton botSpeedButton() {
            return botSpeedButton;
        }

        public MonopolyButton languageButton() {
            return languageButton;
        }

        public GameSessionState sessionState() {
            return sessionState;
        }

        public DebugController debugController() {
            return debugController;
        }

        public BotTurnScheduler botTurnScheduler() {
            return botTurnScheduler;
        }

        public Board board() {
            return Game.this.getBoard();
        }

        public SessionState projectedSessionState() {
            return Game.this.projectedSessionState();
        }

        public Players players() {
            return Game.this.players();
        }

        public Dices dices() {
            return Game.this.dices();
        }

        public Animations animations() {
            return Game.this.animations();
        }

        public DebtController debtController() {
            return Game.this.debtController();
        }

        public GameSidebarPresenter.SidebarState sidebarState() {
            return createSidebarState();
        }

        public LayoutMetrics layoutMetrics() {
            return getLayoutMetrics();
        }

        public float sidebarHistoryHeight() {
            return getSidebarHistoryHeight();
        }

        public float sidebarHistoryPanelY() {
            return getSidebarHistoryPanelY();
        }

        public float sidebarContentTop() {
            return getSidebarContentTop();
        }

        public void updateSidebarControlPositions() {
            Game.this.updateSidebarControlPositions();
        }

        public void enforcePrimaryTurnControlInvariant() {
            Game.this.enforcePrimaryTurnControlInvariant();
        }

        public void showRollDiceControl() {
            Game.this.showRollDiceControl();
        }

        public void showEndTurnControl() {
            Game.this.showEndTurnControl();
        }

        public void updateDebugButtons() {
            Game.this.updateDebugButtons();
        }

        public void refreshButtonInteractivityState() {
            Game.this.refreshButtonInteractivityState();
        }

        public void togglePause() {
            Game.this.togglePause();
        }

        public void runComputerPlayerStep() {
            Game.this.runComputerPlayerStep();
        }

        public void syncTransientPresentationState() {
            Game.this.syncTransientPresentationState();
        }

        public void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
            Game.this.applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning);
        }

        public void endRound(boolean switchTurns) {
            Game.this.endRound(switchTurns);
        }

        public void handlePaymentRequest(
                PaymentRequest request,
                TurnContinuationState continuationState,
                CallbackAction onResolved
        ) {
            Game.this.handlePaymentRequest(request, continuationState, onResolved);
        }

        public GameView createGameView(Player player) {
            return Game.this.createGameView(player);
        }

        public PlayerView createPlayerView(Player player) {
            return Game.this.createPlayerView(player);
        }
    }

}
