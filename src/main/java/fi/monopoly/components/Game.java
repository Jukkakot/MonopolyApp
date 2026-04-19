package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
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
import fi.monopoly.presentation.game.*;
import fi.monopoly.presentation.game.DebugController;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
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
    private final LocalSessionActions localSessionActions;
    private final TurnEngine turnEngine = new TurnEngine();
    private final GameSessionStateCoordinator gameSessionStateCoordinator = new GameSessionStateCoordinator();
    private final GameBotTurnControlCoordinator gameBotTurnControlCoordinator = new GameBotTurnControlCoordinator();
    private final GameDesktopAssemblyFactory gameDesktopAssemblyFactory = new GameDesktopAssemblyFactory();
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
    private final GameSessionState sessionState = new GameSessionState();
    private final BotTurnScheduler botTurnScheduler = new BotTurnScheduler();
    private final GameBotTurnDriver botTurnDriver = new GameBotTurnDriver(botTurnScheduler);
    private final DebugPerformanceStats debugPerformanceStats = new DebugPerformanceStats();
    private LayoutMetrics frameLayoutMetrics;
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
    private GameFrameCoordinator gameFrameCoordinator;
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
        new GameButtonLayoutFactory().apply(
                runtime,
                new GameButtonLayoutFactory.Buttons(
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
                )
        );
        GameDesktopAssemblyFactory.GameDesktopAssembly desktopAssembly = gameDesktopAssemblyFactory.create(
                runtime,
                restoredSessionState,
                LOCAL_SESSION_ID,
                new GameButtonLayoutFactory.Buttons(
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
                turnEngine,
                createGameRuntimeAssemblyHooks(),
                createGameSessionBridgeHooks(),
                createGamePresentationHooks(),
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
        this.auctionViewAdapter = desktopAssembly.auctionViewAdapter();
        this.tradeViewAdapter = desktopAssembly.tradeViewAdapter();
        this.pendingDecisionPopupAdapter = desktopAssembly.pendingDecisionPopupAdapter();
        this.propertyPurchaseFlow = desktopAssembly.propertyPurchaseFlow();
        this.tradeController = desktopAssembly.tradeController();
        this.sessionViewFacade = createSessionViewFacade();
        applyRestoredSessionState(restoredSessionState);
        this.gameControlLayout = desktopAssembly.gameControlLayout();
        this.gamePrimaryTurnControls = desktopAssembly.gamePrimaryTurnControls();
        this.gameSessionQueries = desktopAssembly.gameSessionQueries();
        this.gameTurnFlowCoordinator = desktopAssembly.gameTurnFlowCoordinator();
        this.gameUiController = desktopAssembly.gameUiController();
        this.gamePresentationSupport = desktopAssembly.gamePresentationSupport();
        this.gameBotTurnHooks = desktopAssembly.gameBotTurnHooks();
        this.gameFrameCoordinator = new GameFrameCoordinator(
                runtime,
                gameControlLayout,
                gameSidebarPresenter,
                gamePresentationSupport,
                gamePrimaryTurnControls,
                gameSidebarStateFactory,
                gameSessionStateCoordinator,
                botTurnDriver,
                botTurnScheduler,
                debugPerformanceStats,
                List.of(
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
                )
        );
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
                return sessionState.paused();
            }

            @Override
            public boolean gameOver() {
                return sessionState.gameOver();
            }

            @Override
            public Player winner() {
                return sessionState.winner();
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
                return !sessionState.gameOver() && !runtime.popupService().isAnyVisible() && debtController.debtState() == null;
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

    private GameRuntimeAssemblyFactory.Hooks createGameRuntimeAssemblyHooks() {
        return new GameRuntimeAssemblyFactory.Hooks() {
            @Override
            public void refreshLabels() {
                Game.this.refreshLabels();
            }

            @Override
            public fi.monopoly.components.event.MonopolyEventListener eventListener() {
                return Game.this;
            }

            @Override
            public void rollDice() {
                Game.this.rollDice();
            }

            @Override
            public void setupDefaultGameState(Board board, Players players) {
                Game.this.setupDebugGameConfigs(runtime, board, players);
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
            public void onDebtStateChanged() {
                Game.this.onDebtStateChanged();
            }

            @Override
            public void declareWinner(Player winner) {
                Game.this.declareWinner(winner);
            }

            @Override
            public void debugResetTurnState() {
                Game.this.debugResetTurnState();
            }

            @Override
            public void restoreNormalTurnControls() {
                Game.this.restoreNormalTurnControls();
            }

            @Override
            public void retryPendingDebtPaymentAction() {
                Game.this.retryPendingDebtPaymentAction();
            }

            @Override
            public void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
                Game.this.handlePaymentRequest(request, null, onResolved);
            }

            @Override
            public boolean debtActive() {
                return debtController != null && debtController.debtState() != null;
            }

            @Override
            public boolean gameOver() {
                return sessionState.gameOver();
            }

            @Override
            public int goMoneyAmount() {
                return goMoneyAmount;
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
                return sessionState.gameOver();
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
                return sessionState.paused();
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

    private void applyRestoredSessionState(SessionState restoredSessionState) {
        gameSessionStateCoordinator.restoreSessionState(
                sessionState,
                restoredSessionState,
                sessionApplicationService,
                this::playerById
        );
    }

    private void initializeSessionPresentation(SessionState restoredSessionState) {
        gameSessionStateCoordinator.initializePresentation(
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
                return sessionState.gameOver();
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
        gameSessionStateCoordinator.showPersistenceNotice(sessionState, notice, runtime.app().millis());
    }

    void refreshProjectedSessionState() {
        sessionApplicationService.handle(new RefreshSessionViewCommand(LOCAL_SESSION_ID));
    }

    DebtController debtController() {
        return debtController;
    }

    private void onDebtStateChanged() {
        gameSessionStateCoordinator.onDebtStateChanged(
                sessionApplicationService,
                this::updateDebtButtons,
                () -> restoreBotTurnControlsIfNeeded()
        );
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
        gameFrameCoordinator.drawFrame(createFrameHooks());
    }

    private LayoutMetrics updateFrameLayoutMetrics() {
        frameLayoutMetrics = gameFrameCoordinator.updateFrameLayoutMetrics();
        return frameLayoutMetrics;
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
        frameLayoutMetrics = gameFrameCoordinator.getLayoutMetrics();
        return frameLayoutMetrics;
    }

    private void updateSidebarControlPositions() {
        gameFrameCoordinator.updateSidebarControlPositions();
        frameLayoutMetrics = gameFrameCoordinator.getLayoutMetrics();
    }

    private void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        gameFrameCoordinator.updateSidebarControlPositions(layoutMetrics);
        frameLayoutMetrics = gameFrameCoordinator.getLayoutMetrics();
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
        if (gamePresentationSupport == null) {
            return;
        }
        gameFrameCoordinator.updateDebtButtons(
                debtController.debtState(),
                sessionApplicationService != null ? sessionApplicationService.currentState() : null
        );
    }

    private void updateDebugButtons() {
        if (gamePresentationSupport == null) {
            return;
        }
        gameFrameCoordinator.updatePersistentButtons(sessionState.gameOver());
    }

    private void refreshButtonInteractivityState() {
        gameFrameCoordinator.refreshButtonInteractivityState();
    }

    private void refreshLabels() {
        if (gamePresentationSupport == null) {
            return;
        }
        gameFrameCoordinator.refreshLabels(sessionState.paused(), sessionState.botSpeedMode());
    }

    private void togglePause() {
        if (!gameSessionStateCoordinator.togglePause(sessionState, this::refreshLabels)) {
            return;
        }
        log.info("Game paused={}", sessionState.paused());
    }

    private void cycleBotSpeedMode() {
        BotTurnScheduler.SpeedMode nextMode = gameSessionStateCoordinator.cycleBotSpeedMode(
                sessionState,
                botTurnScheduler::markReadyNow,
                runtime.app().millis(),
                this::refreshLabels
        );
        log.info("Bot speed mode={}", nextMode);
    }

    private void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    private void debugResetTurnState() {
        log.debug("Debug action: reset turn state");
        gameSessionStateCoordinator.debugResetTurnState(new GameSessionStateCoordinator.DebugResetHooks() {
            @Override
            public void finishAllAnimations() {
                animations.finishAllAnimations();
            }

            @Override
            public void resetTransientTurnState() {
                gameTurnFlowCoordinator.resetTransientTurnState();
            }

            @Override
            public void clearDebtState() {
                debtController.clearDebtState();
            }

            @Override
            public void updateDebtButtons() {
                Game.this.updateDebtButtons();
            }

            @Override
            public void hideAllPopups() {
                runtime.popupService().hideAll();
            }

            @Override
            public void showRollDiceControl() {
                Game.this.showRollDiceControl();
            }

            @Override
            public void showDebugResetMessage() {
                runtime.popupService().show(text("game.debug.reset"));
            }
        });
    }

    private void restoreNormalTurnControls() {
        log.trace("Restoring normal turn controls");
        gameSessionStateCoordinator.restoreNormalTurnControls(
                debtController::clearDebtState,
                this::showRollDiceControl
        );
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
        gameSessionStateCoordinator.declareWinner(sessionState, winningPlayer, new GameSessionStateCoordinator.WinnerHooks() {
            @Override
            public void resetTransientTurnState() {
                gameTurnFlowCoordinator.resetTransientTurnState();
            }

            @Override
            public void clearDebtState() {
                debtController.clearDebtState();
            }

            @Override
            public void updateDebtButtons() {
                Game.this.updateDebtButtons();
            }

            @Override
            public void hidePrimaryTurnControls() {
                Game.this.hidePrimaryTurnControls();
            }

            @Override
            public void refreshLabels() {
                Game.this.refreshLabels();
            }

            @Override
            public void focusWinner(Player winner) {
                if (winner.getSpot() != null) {
                    winner.setCoords(winner.getSpot().getTokenCoords(winner));
                    players.focusPlayer(winner);
                }
            }

            @Override
            public void updateLogTurnContext() {
                Game.this.updateLogTurnContext();
            }

            @Override
            public void showVictoryPopup(Player winner) {
                String winnerName = winner != null ? winner.getName() : text("game.bankruptcy.noWinner");
                log.info("Game over. winner={}", winnerName);
                runtime.popupService().show(text("game.victory.popup", winnerName), () -> {
                });
            }
        });
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
        return new GameFrameCoordinator.FrameHooks() {
            @Override
            public GameSessionState sessionState() {
                return sessionState;
            }

            @Override
            public Board board() {
                return board;
            }

            @Override
            public Players players() {
                return players;
            }

            @Override
            public Dices dices() {
                return dices;
            }

            @Override
            public Animations animations() {
                return animations;
            }

            @Override
            public Player turnPlayer() {
                return players != null ? players.getTurn() : null;
            }

            @Override
            public List<String> recentPopupMessages() {
                return runtime.popupService().recentPopupMessages();
            }

            @Override
            public DebtState debtState() {
                return debtController.debtState();
            }

            @Override
            public Player debtDebtor() {
                return debtController.debtState() != null ? debtController.debtState().paymentRequest().debtor() : null;
            }

            @Override
            public boolean popupVisible() {
                return runtime.popupService().isAnyVisible();
            }

            @Override
            public boolean debtSidebarMode() {
                return isDebtSidebarMode();
            }

            @Override
            public boolean endRoundVisible() {
                return endRoundButton.isVisible();
            }

            @Override
            public boolean rollDiceVisible() {
                return dices.isVisible();
            }

            @Override
            public void focusPlayer(Player player) {
                players.focusPlayer(player);
            }

            @Override
            public void restoreBotTurnControlsIfNeeded() {
                Game.this.restoreBotTurnControlsIfNeeded();
            }

            @Override
            public GameBotTurnDriver.Hooks botTurnHooks() {
                return gameBotTurnHooks;
            }
        };
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
        return gameBotTurnControlCoordinator.projectedAction(createBotTurnControlHooks(), currentPlayer)
                == GameBotTurnControlCoordinator.BotPrimaryAction.ROLL_DICE;
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        if (gamePrimaryTurnControls.isEndTurnActionAvailable(
                runtime.popupService().isAnyVisible(),
                debtController.debtState() != null,
                currentPlayer
        )) {
            return true;
        }
        return gameBotTurnControlCoordinator.projectedAction(createBotTurnControlHooks(), currentPlayer)
                == GameBotTurnControlCoordinator.BotPrimaryAction.END_TURN;
    }

    private boolean isProjectedRollDiceActionAvailable() {
        return isRollDiceActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean isProjectedEndTurnActionAvailable() {
        return isEndTurnActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean restoreBotTurnControlsIfNeeded() {
        Player currentPlayer = players != null ? players.getTurn() : null;
        return gameBotTurnControlCoordinator.restoreControlsIfNeeded(createBotTurnControlHooks(), currentPlayer);
    }

    private GameBotTurnControlCoordinator.Hooks createBotTurnControlHooks() {
        return new GameBotTurnControlCoordinator.Hooks() {
            @Override
            public boolean gameOver() {
                return sessionState.gameOver();
            }

            @Override
            public boolean popupVisible() {
                return runtime.popupService().isAnyVisible();
            }

            @Override
            public boolean debtActive() {
                return debtController.debtState() != null;
            }

            @Override
            public boolean animationsRunning() {
                return animations.isRunning();
            }

            @Override
            public boolean activeAuctionOpen() {
                return sessionApplicationService.hasActiveAuction();
            }

            @Override
            public boolean activeTradeOpen() {
                return sessionApplicationService.hasActiveTrade();
            }

            @Override
            public boolean auctionOverrideActive() {
                return sessionApplicationService.hasAuctionOverride();
            }

            @Override
            public boolean tradeOverrideActive() {
                return sessionApplicationService.hasTradeOverride();
            }

            @Override
            public boolean pendingDecisionOverrideActive() {
                return sessionApplicationService.hasPendingDecisionOverride();
            }

            @Override
            public fi.monopoly.types.DiceState currentDiceState() {
                return dices.getValue() != null ? dices.getValue().diceState() : null;
            }

            @Override
            public boolean rollDiceActionAlreadyAvailable() {
                return gamePrimaryTurnControls.isRollDiceActionAvailable(
                        runtime.popupService().isAnyVisible(),
                        debtController.debtState() != null,
                        players != null ? players.getTurn() : null
                );
            }

            @Override
            public boolean endTurnActionAlreadyAvailable() {
                return gamePrimaryTurnControls.isEndTurnActionAvailable(
                        runtime.popupService().isAnyVisible(),
                        debtController.debtState() != null,
                        players != null ? players.getTurn() : null
                );
            }

            @Override
            public void showRollDiceControl() {
                Game.this.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                Game.this.showEndTurnControl();
            }
        };
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
