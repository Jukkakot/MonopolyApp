package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.application.session.turn.TurnContinuationGateway;
import fi.monopoly.components.animation.Animation;
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
import fi.monopoly.presentation.game.*;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import fi.monopoly.text.UiTexts;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.UiTokens;
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
    private void setupDebugGameConfigs(MonopolyRuntime runtime) {
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
    private final LocalSessionActions localSessionActions;
    private final TurnEngine turnEngine = new TurnEngine();
    private final LegacyGameRuntimeBootstrapper legacyGameRuntimeBootstrapper = new LegacyGameRuntimeBootstrapper();
    private final RestoredSessionReattachmentCoordinator restoredSessionReattachmentCoordinator =
            new RestoredSessionReattachmentCoordinator();
    private final SessionApplicationService sessionApplicationService;
    private final PropertyPurchaseFlow propertyPurchaseFlow;
    private final PendingDecisionPopupAdapter pendingDecisionPopupAdapter;
    private final DebtActionDispatcher debtActionDispatcher;
    private final AuctionViewAdapter auctionViewAdapter;
    private final TradeViewAdapter tradeViewAdapter;
    private TradeController tradeController;
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

    // Mutable game state
    private Players players;
    private Dices dices;
    private Animations animations;
    private int goMoneyAmount = 200;
    private Board board;
    private boolean paused;
    private boolean gameOver;
    private Player winner;
    private final BotTurnScheduler botTurnScheduler = new BotTurnScheduler();
    private final GameBotTurnDriver botTurnDriver = new GameBotTurnDriver(botTurnScheduler);
    private BotTurnScheduler.SpeedMode botSpeedMode = BotTurnScheduler.SpeedMode.NORMAL;
    private final DebugPerformanceStats debugPerformanceStats = new DebugPerformanceStats();
    private LayoutMetrics frameLayoutMetrics;
    private long lastAnimationUpdateNanos = -1L;
    private final SessionViewFacade sessionViewFacade;
    private GameControlLayout gameControlLayout;
    private GamePrimaryTurnControls gamePrimaryTurnControls;
    private GameSessionQueries gameSessionQueries;
    private final GameSidebarStateFactory gameSidebarStateFactory = new GameSidebarStateFactory();
    private final GameSidebarPresenter gameSidebarPresenter;
    private final GameUiController gameUiController;
    private GamePresentationSupport gamePresentationSupport;
    private GameTurnFlowCoordinator gameTurnFlowCoordinator;
    private GameBotTurnDriver.Hooks gameBotTurnHooks;
    private String persistenceNotice;
    private int persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;

    public Game(MonopolyRuntime runtime) {
        this(runtime, null, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState) {
        this(runtime, restoredSessionState, LocalSessionActions.NO_OP_ACTIONS);
    }

    public Game(MonopolyRuntime runtime, SessionState restoredSessionState, LocalSessionActions localSessionActions) {
        this.runtime = runtime;
        this.localSessionActions = localSessionActions;
        this.endRoundButton = new MonopolyButton(runtime, "endRound");
        this.retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        this.declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        this.debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        this.pauseButton = new MonopolyButton(runtime, "pause");
        this.tradeButton = new MonopolyButton(runtime, "trade");
        this.saveButton = new MonopolyButton(runtime, "save");
        this.loadButton = new MonopolyButton(runtime, "load");
        this.botSpeedButton = new MonopolyButton(runtime, "botSpeed");
        this.languageButton = new MonopolyButton(runtime, "language");
        this.gameSidebarPresenter = new GameSidebarPresenter(runtime);
        setupButtons();
        setupRuntimeDependencies(restoredSessionState);
        setupControllers();
        GameSessionBridgeFactory.GameSessionBridge sessionBridge = new GameSessionBridgeFactory(runtime).create(
                LOCAL_SESSION_ID,
                players,
                dices,
                debtController,
                createGameSessionBridgeHooks()
        );
        this.sessionApplicationService = sessionBridge.sessionApplicationService();
        this.debtActionDispatcher = sessionBridge.debtActionDispatcher();
        this.auctionViewAdapter = sessionBridge.auctionViewAdapter();
        this.tradeViewAdapter = sessionBridge.tradeViewAdapter();
        this.pendingDecisionPopupAdapter = sessionBridge.pendingDecisionPopupAdapter();
        this.propertyPurchaseFlow = pendingDecisionPopupAdapter;
        this.tradeController = sessionBridge.tradeController();
        this.sessionViewFacade = createSessionViewFacade();
        applyRestoredSessionState(restoredSessionState);
        registerGameSession();
        GamePresentationFactory.GamePresentationBundle presentationBundle = new GamePresentationFactory().create(
                runtime,
                new GamePresentationFactory.Buttons(
                        endRoundButton,
                        retryDebtButton,
                        declareBankruptcyButton,
                        debugGodModeButton,
                        pauseButton,
                        tradeButton,
                        saveButton,
                        loadButton,
                        botSpeedButton,
                        languageButton
                ),
                new GamePresentationFactory.Dependencies(
                        players,
                        dices,
                        board,
                        animations,
                        turnEngine,
                        sessionApplicationService,
                        pendingDecisionPopupAdapter,
                        propertyPurchaseFlow,
                        debtActionDispatcher,
                        debtController,
                        debugController,
                        auctionViewAdapter,
                        tradeViewAdapter,
                        tradeController,
                        debugPerformanceStats
                ),
                createGamePresentationHooks()
        );
        this.gameControlLayout = presentationBundle.gameControlLayout();
        this.gamePrimaryTurnControls = presentationBundle.gamePrimaryTurnControls();
        this.gameSessionQueries = presentationBundle.gameSessionQueries();
        this.gameTurnFlowCoordinator = presentationBundle.gameTurnFlowCoordinator();
        this.gameUiController = presentationBundle.gameUiController();
        this.gamePresentationSupport = presentationBundle.gamePresentationSupport();
        this.gameBotTurnHooks = presentationBundle.gameBotTurnHooks();
        initializeSessionPresentation(restoredSessionState);
        setupButtonActions();

        if (FORCE_DEBT_DEBUG_SCENARIO) {
            debugController.initializeDebtDebugScenario();
        }
    }

    private GameSessionBridgeFactory.Hooks createGameSessionBridgeHooks() {
        return new GameSessionBridgeFactory.Hooks() {
            @Override
            public boolean paused() {
                return paused;
            }

            @Override
            public boolean gameOver() {
                return gameOver;
            }

            @Override
            public Player winner() {
                return winner;
            }

            @Override
            public boolean projectedRollDiceActionAvailable() {
                return Game.this.isProjectedRollDiceActionAvailable();
            }

            @Override
            public boolean projectedEndTurnActionAvailable() {
                return Game.this.isProjectedEndTurnActionAvailable();
            }

            @Override
            public void endTurn() {
                Game.this.endRound(true);
            }

            @Override
            public Player playerById(String playerId) {
                return Game.this.playerById(playerId);
            }

            @Override
            public boolean computerTurn() {
                return players != null && players.getTurn() != null && players.getTurn().isComputerControlled();
            }

            @Override
            public boolean canOpenTrade() {
                return !gameOver && !runtime.popupService().isAnyVisible() && debtController.debtState() == null;
            }

            @Override
            public Player currentTurnPlayer() {
                return players != null ? players.getTurn() : null;
            }

            @Override
            public List<Player> players() {
                return players != null ? players.getPlayers() : List.of();
            }
        };
    }

    private SessionViewFacade createSessionViewFacade() {
        return new SessionViewFacade(
                runtime,
                players,
                board,
                () -> debtController != null ? debtController.debtState() : null,
                retryDebtButton::isVisible,
                declareBankruptcyButton::isVisible,
                this::isRollDiceActionAvailable,
                this::isEndTurnActionAvailable,
                () -> gameSessionQueries.countUnownedProperties(),
                player -> gameSessionQueries.calculateBoardDangerScore(player)
        );
    }

    private GamePresentationFactory.Hooks createGamePresentationHooks() {
        return new GamePresentationFactory.Hooks() {
            @Override
            public boolean gameOver() {
                return gameOver;
            }

            @Override
            public boolean debtActive() {
                return debtController.debtState() != null;
            }

            @Override
            public boolean popupVisible() {
                return runtime.popupService().isAnyVisible();
            }

            @Override
            public boolean paused() {
                return paused;
            }

            @Override
            public int goMoneyAmount() {
                return goMoneyAmount;
            }

            @Override
            public int nowMillis() {
                return runtime.app().millis();
            }

            @Override
            public List<Locale> supportedLocales() {
                return SUPPORTED_UI_LOCALES;
            }

            @Override
            public String sessionId() {
                return LOCAL_SESSION_ID;
            }

            @Override
            public Player currentTurnPlayer() {
                return players != null ? players.getTurn() : null;
            }

            @Override
            public GameView createCurrentGameView() {
                return createGameView(players.getTurn());
            }

            @Override
            public PlayerView createCurrentPlayerView() {
                return createPlayerView(players.getTurn());
            }

            @Override
            public void updateLogTurnContext() {
                Game.this.updateLogTurnContext();
            }

            @Override
            public void hidePrimaryTurnControls() {
                Game.this.hidePrimaryTurnControls();
            }

            @Override
            public void showRollDiceControl() {
                Game.this.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                Game.this.showEndTurnControl();
            }

            @Override
            public void syncTransientPresentationState() {
                Game.this.syncTransientPresentationState();
            }

            @Override
            public void togglePause() {
                Game.this.togglePause();
            }

            @Override
            public void cycleBotSpeedMode() {
                Game.this.cycleBotSpeedMode();
            }

            @Override
            public void switchLanguage(Locale locale) {
                Game.this.switchLanguage(locale);
            }

            @Override
            public void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind) {
                Game.this.scheduleNextComputerAction(delayKind, runtime.app().millis());
            }

            @Override
            public void handlePaymentRequest(PaymentRequest request, fi.monopoly.domain.session.TurnContinuationState continuationState, CallbackAction onResolved) {
                Game.this.handlePaymentRequest(request, continuationState, onResolved);
            }

            @Override
            public boolean projectedRollDiceActionAvailable() {
                return Game.this.isProjectedRollDiceActionAvailable();
            }

            @Override
            public boolean projectedEndTurnActionAvailable() {
                return Game.this.isProjectedEndTurnActionAvailable();
            }

            @Override
            public boolean restoreBotTurnControlsIfNeeded() {
                return Game.this.restoreBotTurnControlsIfNeeded();
            }

            @Override
            public Runnable saveSessionAction() {
                return localSessionActions.saveSession();
            }

            @Override
            public Runnable loadSessionAction() {
                return localSessionActions.loadSession();
            }
        };
    }

    private void setupButtons() {
        LayoutMetrics defaultLayout = LayoutMetrics.defaultWindow();
        endRoundButton.setPosition(defaultLayout.sidebarX() + UiTokens.sidebarValueX(), defaultLayout.sidebarPrimaryButtonY());
        endRoundButton.setSize(150, 44);
        endRoundButton.setAutoWidth(100, 28, 180);

        retryDebtButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), defaultLayout.sidebarPrimaryButtonY());
        retryDebtButton.setSize(140, 40);
        retryDebtButton.setAutoWidth(140, 28, 220);

        declareBankruptcyButton.setPosition(defaultLayout.sidebarX() + UiTokens.sidebarValueX(), defaultLayout.sidebarPrimaryButtonY());
        declareBankruptcyButton.setSize(140, 40);
        declareBankruptcyButton.setAutoWidth(140, 28, 220);

        debugGodModeButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), defaultLayout.sidebarDebugButtonRow1Y());
        debugGodModeButton.setSize(300, 36);
        debugGodModeButton.setAutoWidth(180, 28, 300);

        pauseButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 96);
        pauseButton.setSize(140, 36);
        pauseButton.setAutoWidth(120, 28, 180);
        pauseButton.setAllowedDuringComputerTurn(true);

        tradeButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 96);
        tradeButton.setSize(140, 36);
        tradeButton.setAutoWidth(120, 28, 220);

        saveButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        saveButton.setSize(120, 36);
        saveButton.setAutoWidth(100, 28, 180);
        saveButton.setAllowedDuringComputerTurn(true);

        loadButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        loadButton.setSize(120, 36);
        loadButton.setAutoWidth(100, 28, 180);
        loadButton.setAllowedDuringComputerTurn(true);

        botSpeedButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        botSpeedButton.setSize(140, 36);
        botSpeedButton.setAutoWidth(120, 28, 220);
        botSpeedButton.setAllowedDuringComputerTurn(true);

        languageButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        languageButton.setSize(220, 36);
        languageButton.setAutoWidth(180, 28, 280);
        languageButton.setAllowedDuringComputerTurn(true);

        endRoundButton.hide();
        retryDebtButton.hide();
        declareBankruptcyButton.hide();
        debugGodModeButton.hide();
        pauseButton.hide();
        tradeButton.hide();
        saveButton.hide();
        loadButton.hide();
        botSpeedButton.hide();
    }

    private void setupRuntimeDependencies(SessionState restoredSessionState) {
        UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        dices = Dices.setRollDice(runtime, this::rollDice);
        animations = new Animations();
        LegacyGameRuntimeBootstrapper.LegacyGameRuntimeState runtimeState =
                legacyGameRuntimeBootstrapper.bootstrap(runtime, restoredSessionState);
        board = runtimeState.board();
        players = runtimeState.players();
        if (!runtimeState.restoredSession()) {
            setupDefaultGameState();
        }
    }

    private void setupControllers() {
        debtController = new DebtController(
                runtime,
                players,
                this::hidePrimaryTurnControls,
                this::showRollDiceControl,
                this::onDebtStateChanged,
                this::declareWinner
        );
        debugController = new DebugController(
                runtime,
                board,
                () -> players != null ? players.getTurn() : null,
                this::debugResetTurnState,
                this::restoreNormalTurnControls,
                this::retryPendingDebtPaymentAction,
                (request, onResolved) -> handlePaymentRequest(request, null, onResolved)
        );
    }

    private void registerGameSession() {
        runtime.setGameSession(new GameSession(players, dices, animations)
                .withStateSuppliers(
                        () -> debtController != null && debtController.debtState() != null,
                        () -> gameOver,
                        () -> goMoneyAmount
                )
                .withDebtActionDispatcher(debtActionDispatcher));
    }

    private void setupDefaultGameState() {
        setupDebugGameConfigs(runtime);
    }

    private void applyRestoredSessionState(SessionState restoredSessionState) {
        RestoredSessionReattachmentCoordinator.RestoredGameState restoredGameState =
                restoredSessionReattachmentCoordinator.restoreAuthoritativeState(
                        restoredSessionState,
                        sessionApplicationService,
                        this::playerById
                );
        paused = restoredGameState.paused();
        gameOver = restoredGameState.gameOver();
        winner = restoredGameState.winner();
    }

    private void initializeSessionPresentation(SessionState restoredSessionState) {
        restoredSessionReattachmentCoordinator.restorePresentation(
                restoredSessionState,
                sessionApplicationService,
                debtController,
                createRestoredSessionReattachmentHooks()
        );
    }

    private RestoredSessionReattachmentCoordinator.Hooks createRestoredSessionReattachmentHooks() {
        return new RestoredSessionReattachmentCoordinator.Hooks() {
            @Override
            public Player playerById(String playerId) {
                return Game.this.playerById(playerId);
            }

            @Override
            public boolean gameOver() {
                return gameOver;
            }

            @Override
            public void refreshLabels() {
                Game.this.refreshLabels();
            }

            @Override
            public void showRollDiceControl() {
                Game.this.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                Game.this.showEndTurnControl();
            }

            @Override
            public void hidePrimaryTurnControls() {
                Game.this.hidePrimaryTurnControls();
            }

            @Override
            public void updateDebtButtons() {
                Game.this.updateDebtButtons();
            }

            @Override
            public void syncTransientPresentationState() {
                Game.this.syncTransientPresentationState();
            }

            @Override
            public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
                if (continuationState != null && gameTurnFlowCoordinator != null) {
                    gameTurnFlowCoordinator.resumeContinuation(continuationState);
                }
            }
        };
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
        if (notice == null || notice.isBlank()) {
            persistenceNotice = null;
            persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;
            return;
        }
        persistenceNotice = notice;
        persistenceNoticeExpiresAtMillis = runtime.app().millis() + 5000;
    }

    void refreshProjectedSessionState() {
        sessionApplicationService.handle(new RefreshSessionViewCommand(LOCAL_SESSION_ID));
    }

    DebtController debtController() {
        return debtController;
    }

    private void onDebtStateChanged() {
        updateDebtButtons();
        if (sessionApplicationService != null) {
            sessionApplicationService.clearActiveDebtOverride();
        }
        restoreBotTurnControlsIfNeeded();
    }

    private void setupButtonActions() {
        gameUiController.bindButtonActions();
    }

    Players players() {
        return players;
    }

    Dices dices() {
        return dices;
    }

    Animations animations() {
        return animations;
    }

    public void draw() {
        long frameStart = System.nanoTime();
        updateLogTurnContext();
        LayoutMetrics layoutMetrics = updateFrameLayoutMetrics();
        boolean hasSidebarSpace = layoutMetrics.hasSidebarSpace();
        GameSidebarPresenter.SidebarState sidebarState = createSidebarState();
        boolean animationWasRunning = animations.isRunning();
        float animationDeltaSeconds = resolveAnimationDeltaSeconds(frameStart);
        if (MonopolyApp.SKIP_ANNIMATIONS) {
            animations.finishAllAnimations();
        }
        if (!runtime.popupService().isAnyVisible()) {
            animations.updateAnimations(animationDeltaSeconds);
        }
        applyComputerActionCooldownIfAnimationJustFinished(animationWasRunning);
        updateSidebarControlPositions(layoutMetrics);
        board.draw(null);
        if (hasSidebarSpace) {
            drawSidebarPanel(layoutMetrics, sidebarState);
        }
        if (!isDebtSidebarMode()) {
            dices.draw(null);
        }
        if (hasSidebarSpace && isDebtSidebarMode()) {
            players.focusPlayer(debtController.debtState().paymentRequest().debtor());
        }
        if (hasSidebarSpace) {
            players.draw(gameSidebarPresenter.contentTop(layoutMetrics, sidebarState), !isDebtSidebarMode(), !isDebtSidebarMode());
        } else {
            players.drawTokens();
        }
        updateDebugButtons();
        enforcePrimaryTurnControlInvariant();
        if (hasSidebarSpace) {
            drawDebtState(layoutMetrics, sidebarState);
        }
        syncTransientPresentationState();
        runComputerPlayerStep();
        debugPerformanceStats.recordFrame(System.nanoTime() - frameStart);
    }

    private float resolveAnimationDeltaSeconds(long nowNanos) {
        if (lastAnimationUpdateNanos < 0L) {
            lastAnimationUpdateNanos = nowNanos;
            return Animation.REFERENCE_FRAME_SECONDS;
        }
        float deltaSeconds = (nowNanos - lastAnimationUpdateNanos) / 1_000_000_000f;
        lastAnimationUpdateNanos = nowNanos;
        return deltaSeconds;
    }

    private LayoutMetrics updateFrameLayoutMetrics() {
        frameLayoutMetrics = gameControlLayout.updateFrameLayoutMetrics();
        return frameLayoutMetrics;
    }

    private void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        Player turnPlayer = players.getTurn();
        botTurnScheduler.applyAnimationFinishCooldownIfNeeded(
                animationWasRunning,
                animations.isRunning(),
                turnPlayer != null && turnPlayer.isComputerControlled(),
                runtime.app().millis(),
                botSpeedMode,
                players.getPlayers().stream().allMatch(Player::isComputerControlled)
        );
    }

    /**
     * Draws the persistent right-side information panel so turn state, player
     * overview and controls stay in one predictable area.
     */
    private void drawSidebarPanel(LayoutMetrics layoutMetrics, GameSidebarPresenter.SidebarState sidebarState) {
        gameSidebarPresenter.drawSidebarPanel(layoutMetrics, sidebarState, debugPerformanceStats::recordHistoryLayout);
    }

    private boolean isDebtSidebarMode() {
        return debtController.debtState() != null;
    }

    private void rollDice() {
        gameTurnFlowCoordinator.rollDice();
    }

    private void runComputerPlayerStep() {
        botTurnDriver.step(gameBotTurnHooks);
    }

    private void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        botTurnScheduler.schedule(
                delayKind,
                now,
                botSpeedMode,
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

    private void drawDebtState(LayoutMetrics layoutMetrics, GameSidebarPresenter.SidebarState sidebarState) {
        gameSidebarPresenter.drawDebtState(layoutMetrics, sidebarState.debtState());
    }

    private LayoutMetrics getLayoutMetrics() {
        return frameLayoutMetrics != null ? frameLayoutMetrics : LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
    }

    private void updateSidebarControlPositions() {
        updateSidebarControlPositions(updateFrameLayoutMetrics());
    }

    private void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        gameControlLayout.updateSidebarControlPositions(layoutMetrics);
    }

    private float getSidebarHistoryHeight() {
        return gameControlLayout.historyHeight(getLayoutMetrics());
    }

    private float getSidebarHistoryPanelY() {
        return gameControlLayout.historyPanelY(getLayoutMetrics());
    }

    private float getSidebarReservedTop() {
        return gameControlLayout.reservedTop(getLayoutMetrics());
    }

    private GameSidebarPresenter.SidebarState createSidebarState() {
        clearExpiredPersistenceNoticeIfNeeded();
        return gameSidebarStateFactory.createSidebarState(
                players.getTurn(),
                players.getPlayers(),
                runtime.popupService().recentPopupMessages(),
                debtController.debtState(),
                persistenceNotice,
                gameOver,
                runtime.popupService().isAnyVisible(),
                animations.isRunning(),
                endRoundButton.isVisible(),
                dices.isVisible(),
                getSidebarHistoryPanelY(),
                getSidebarHistoryHeight(),
                getSidebarReservedTop()
        );
    }

    private void clearExpiredPersistenceNoticeIfNeeded() {
        if (persistenceNotice == null) {
            return;
        }
        if (runtime.app().millis() < persistenceNoticeExpiresAtMillis) {
            return;
        }
        persistenceNotice = null;
        persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;
    }

    private float getSidebarContentTop() {
        return gameSidebarPresenter.contentTop(getLayoutMetrics(), createSidebarState());
    }

    private void updateDebtButtons() {
        gamePresentationSupport.updateDebtButtons(
                debtController.debtState(),
                sessionApplicationService != null ? sessionApplicationService.currentState() : null
        );
    }

    private void updateDebugButtons() {
        gamePresentationSupport.updatePersistentButtons(gameOver);
    }

    private void refreshLabels() {
        gamePresentationSupport.refreshLabels(paused, botSpeedMode);
    }

    private void togglePause() {
        if (gameOver) {
            return;
        }
        paused = !paused;
        refreshLabels();
        log.info("Game paused={}", paused);
    }

    private void cycleBotSpeedMode() {
        botSpeedMode = botSpeedMode.next();
        botTurnScheduler.markReadyNow(runtime.app().millis());
        refreshLabels();
        log.info("Bot speed mode={}", botSpeedMode);
    }

    private void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    private void debugResetTurnState() {
        log.debug("Debug action: reset turn state");
        animations.finishAllAnimations();
        gameTurnFlowCoordinator.resetTransientTurnState();
        debtController.clearDebtState();
        updateDebtButtons();
        runtime.popupService().hideAll();
        showRollDiceControl();
        runtime.popupService().show(text("game.debug.reset"));
    }

    private void restoreNormalTurnControls() {
        log.trace("Restoring normal turn controls");
        debtController.clearDebtState();
        showRollDiceControl();
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
        gameOver = true;
        winner = winningPlayer;
        paused = false;
        gameTurnFlowCoordinator.resetTransientTurnState();
        debtController.clearDebtState();
        updateDebtButtons();
        hidePrimaryTurnControls();
        refreshLabels();
        if (winner != null && winner.getSpot() != null) {
            winner.setCoords(winner.getSpot().getTokenCoords(winner));
            players.focusPlayer(winner);
        }
        String winnerName = winner != null ? winner.getName() : text("game.bankruptcy.noWinner");
        updateLogTurnContext();
        log.info("Game over. winner={}", winnerName);
        runtime.popupService().show(text("game.victory.popup", winnerName), () -> {
        });
    }

    private void updateLogTurnContext() {
        gamePresentationSupport.updateLogTurnContext(gameOver, winner, players != null ? players.getTurn() : null);
    }

    private void syncTransientPresentationState() {
        gamePresentationSupport.syncTransientPresentationState();
        restoreBotTurnControlsIfNeeded();
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
        gamePrimaryTurnControls.enforceInvariant(
                debtController.debtState() != null,
                () -> log.warn("Primary turn controls were both visible. Hiding roll dice button to keep end-turn state authoritative.")
        );
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
        if (gamePrimaryTurnControls.isRollDiceActionAvailable(
                runtime.popupService().isAnyVisible(),
                debtController.debtState() != null,
                currentPlayer
        )) {
            return true;
        }
        return deriveBotTurnActionAvailability(currentPlayer) == GamePrimaryAction.ROLL_DICE;
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        if (gamePrimaryTurnControls.isEndTurnActionAvailable(
                runtime.popupService().isAnyVisible(),
                debtController.debtState() != null,
                currentPlayer
        )) {
            return true;
        }
        return deriveBotTurnActionAvailability(currentPlayer) == GamePrimaryAction.END_TURN;
    }

    private boolean isProjectedRollDiceActionAvailable() {
        return isRollDiceActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean isProjectedEndTurnActionAvailable() {
        return isEndTurnActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean restoreBotTurnControlsIfNeeded() {
        Player currentPlayer = players != null ? players.getTurn() : null;
        if (currentPlayer == null || !currentPlayer.isComputerControlled()) {
            return false;
        }
        if (gameOver
                || runtime.popupService().isAnyVisible()
                || debtController.debtState() != null
                || animations.isRunning()
                || sessionApplicationService.hasActiveAuction()
                || sessionApplicationService.hasActiveTrade()) {
            return false;
        }
        if (isRollDiceActionAvailable(currentPlayer) || isEndTurnActionAvailable(currentPlayer)) {
            return false;
        }
        GamePrimaryAction derivedAction = deriveBotTurnActionAvailability(currentPlayer);
        if (derivedAction == GamePrimaryAction.ROLL_DICE) {
            showRollDiceControl();
            return true;
        }
        if (derivedAction == GamePrimaryAction.END_TURN) {
            showEndTurnControl();
            return true;
        }
        return false;
    }

    private GamePrimaryAction deriveBotTurnActionAvailability(Player currentPlayer) {
        if (currentPlayer == null
                || !currentPlayer.isComputerControlled()
                || runtime.popupService().isAnyVisible()
                || debtController.debtState() != null
                || sessionApplicationService.hasAuctionOverride()
                || sessionApplicationService.hasTradeOverride()
                || sessionApplicationService.hasPendingDecisionOverride()) {
            return GamePrimaryAction.NONE;
        }
        if (dices.getValue() == null || dices.getValue().diceState() == fi.monopoly.types.DiceState.DOUBLES) {
            return GamePrimaryAction.ROLL_DICE;
        }
        return GamePrimaryAction.END_TURN;
    }

    private enum GamePrimaryAction {
        NONE,
        ROLL_DICE,
        END_TURN
    }

    public void dispose() {
        runtime.eventBus().removeListener(this);
        runtime.popupService().hideAll();
        if (players != null) {
            players.dispose();
        }
        if (dices != null) {
            dices.dispose();
        }
        endRoundButton.dispose();
        retryDebtButton.dispose();
        declareBankruptcyButton.dispose();
        debugGodModeButton.dispose();
        pauseButton.dispose();
        tradeButton.dispose();
        saveButton.dispose();
        loadButton.dispose();
        botSpeedButton.dispose();
        languageButton.dispose();
    }

}
