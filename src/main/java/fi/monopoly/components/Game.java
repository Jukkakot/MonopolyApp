package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.*;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.trade.TradeController;
import fi.monopoly.components.trade.TradeOfferEvaluator;
import fi.monopoly.components.trade.TradeUiBuilder;
import fi.monopoly.components.turn.TurnEngine;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.game.BotTurnScheduler;
import fi.monopoly.presentation.game.GameBotTurnDriver;
import fi.monopoly.presentation.game.GameControlLayout;
import fi.monopoly.presentation.game.LegacyTurnActionGatewayAdapter;
import fi.monopoly.presentation.game.GameSidebarPresenter;
import fi.monopoly.presentation.game.GameTurnFlowCoordinator;
import fi.monopoly.presentation.game.GameUiController;
import fi.monopoly.presentation.game.SessionBackedComputerTurnContext;
import fi.monopoly.presentation.game.SessionViewFacade;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.session.projection.LegacySessionProjector;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.LegacyTradeGateway;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import fi.monopoly.text.UiTexts;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.UiTokens;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import processing.event.Event;

import java.util.*;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
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
    private final TurnEngine turnEngine = new TurnEngine();
    private final SessionApplicationService sessionApplicationService;
    private final PropertyPurchaseFlow propertyPurchaseFlow;
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
    private final GameSidebarPresenter gameSidebarPresenter;
    private final GameUiController gameUiController;
    private GameTurnFlowCoordinator gameTurnFlowCoordinator;
    private PrimaryTurnControlState primaryTurnControlState = PrimaryTurnControlState.NONE;

    public Game(MonopolyRuntime runtime) {
        this.runtime = runtime;
        this.endRoundButton = new MonopolyButton(runtime, "endRound");
        this.retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        this.declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        this.debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        this.pauseButton = new MonopolyButton(runtime, "pause");
        this.tradeButton = new MonopolyButton(runtime, "trade");
        this.botSpeedButton = new MonopolyButton(runtime, "botSpeed");
        this.languageButton = new MonopolyButton(runtime, "language");
        this.gameUiController = new GameUiController(
                endRoundButton,
                retryDebtButton,
                declareBankruptcyButton,
                debugGodModeButton,
                pauseButton,
                tradeButton,
                botSpeedButton,
                languageButton,
                SUPPORTED_UI_LOCALES,
                new GameUiHooks()
        );
        this.gameSidebarPresenter = new GameSidebarPresenter(runtime);
        setupButtons();
        setupRuntimeDependencies();
        setupControllers();
        this.sessionApplicationService = new SessionApplicationService(
                LOCAL_SESSION_ID,
                new LegacySessionProjector(
                        LOCAL_SESSION_ID,
                        () -> players,
                        () -> LegacyPopupSnapshot.fromPopupService(runtime.popupService()),
                        () -> debtController != null ? debtController.debtState() : null,
                        () -> paused,
                        () -> gameOver,
                        () -> winner,
                        this::isProjectedRollDiceActionAvailable,
                        this::isProjectedEndTurnActionAvailable
                )::project
        );
        this.sessionApplicationService.configureRentAndDebtFlow(debtController);
        this.sessionApplicationService.configureAuctionFlow(runtime.popupService(), players);
        this.sessionApplicationService.configurePropertyPurchaseFlow(runtime.popupService(), players);
        this.sessionApplicationService.configureTradeFlow(() -> players != null ? players.getPlayers() : List.of());
        this.sessionApplicationService.configureTurnActionFlow(
                new LegacyTurnActionGatewayAdapter(dices, () -> players != null ? players.getTurn() : null, () -> endRound(true))
        );
        this.debtActionDispatcher = new DebtActionDispatcher(
                LOCAL_SESSION_ID,
                sessionApplicationService,
                runtime.popupService(),
                () -> players != null ? players.getTurn() : null
        );
        this.auctionViewAdapter = new AuctionViewAdapter(
                LOCAL_SESSION_ID,
                sessionApplicationService,
                runtime.popupService(),
                players
        );
        LegacyTradeGateway legacyTradeGateway = new LegacyTradeGateway(() -> players != null ? players.getPlayers() : List.of());
        this.tradeViewAdapter = new TradeViewAdapter(
                LOCAL_SESSION_ID,
                sessionApplicationService,
                runtime.popupService(),
                legacyTradeGateway,
                new TradeUiBuilder(new TradeOfferEvaluator()),
                () -> players != null && players.getTurn() != null && players.getTurn().isComputerControlled()
        );
        this.propertyPurchaseFlow = new PendingDecisionPopupAdapter(
                LOCAL_SESSION_ID,
                sessionApplicationService,
                runtime.popupService(),
                sessionApplicationService::openPropertyPurchaseDecision,
                auctionViewAdapter::sync
        );
        this.tradeController = new TradeController(
                runtime,
                LOCAL_SESSION_ID,
                sessionApplicationService,
                tradeViewAdapter,
                legacyTradeGateway,
                () -> !gameOver && !runtime.popupService().isAnyVisible() && debtController.debtState() == null,
                () -> players != null ? players.getTurn() : null,
                () -> players != null ? players.getPlayers() : List.of()
        );
        this.sessionViewFacade = new SessionViewFacade(
                runtime,
                players,
                board,
                () -> debtController != null ? debtController.debtState() : null,
                retryDebtButton::isVisible,
                declareBankruptcyButton::isVisible,
                this::isRollDiceActionAvailable,
                this::isEndTurnActionAvailable,
                this::countUnownedProperties,
                this::calculateBoardDangerScore
        );
        registerGameSession();
        setupDefaultGameState();
        this.gameControlLayout = new GameControlLayout(
                runtime,
                endRoundButton,
                retryDebtButton,
                declareBankruptcyButton,
                debugGodModeButton,
                pauseButton,
                tradeButton,
                botSpeedButton,
                languageButton,
                dices
        );
        this.gameTurnFlowCoordinator = new GameTurnFlowCoordinator(
                runtime,
                players,
                dices,
                board,
                animations,
                turnEngine,
                propertyPurchaseFlow,
                () -> goMoneyAmount,
                new GameTurnFlowHooks()
        );
        showRollDiceControl();
        setupButtonActions();

        if (FORCE_DEBT_DEBUG_SCENARIO) {
            debugController.initializeDebtDebugScenario();
        }
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

        botSpeedButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        botSpeedButton.setSize(140, 36);
        botSpeedButton.setAutoWidth(120, 28, 220);
        botSpeedButton.setAllowedDuringComputerTurn(true);

        languageButton.setPosition(defaultLayout.sidebarX() + UiTokens.spacingMd(), runtime.app().height - 48);
        languageButton.setSize(220, 36);
        languageButton.setAutoWidth(180, 28, 280);
        languageButton.setAllowedDuringComputerTurn(true);

        refreshLabels();
        endRoundButton.hide();
        retryDebtButton.hide();
        declareBankruptcyButton.hide();
        debugGodModeButton.hide();
        pauseButton.hide();
        tradeButton.hide();
        botSpeedButton.hide();
    }

    private void setupRuntimeDependencies() {
        UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        PropertyFactory.resetState();
        JailSpot.jailTimeLeftMap.clear();
        board = new Board(runtime);
        dices = Dices.setRollDice(runtime, this::rollDice);
        players = new Players(runtime);
        animations = new Animations();
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
                this::handlePaymentRequest
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

    Board getBoard() {
        return board;
    }

    SessionState projectedSessionState() {
        return sessionApplicationService.currentState();
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

    private String resolveCurrentTurnPhase() {
        if (gameOver) {
            return text("sidebar.phase.gameOver");
        }
        if (debtController.debtState() != null) {
            return text("sidebar.phase.debt");
        }
        if (runtime.popupService().isAnyVisible()) {
            return text("sidebar.phase.popup");
        }
        if (animations.isRunning()) {
            return text("sidebar.phase.animation");
        }
        if (endRoundButton.isVisible()) {
            return text("sidebar.phase.endTurn");
        }
        if (dices.isVisible()) {
            return text("sidebar.phase.roll");
        }
        return text("sidebar.phase.resolving");
    }

    private boolean isDebtSidebarMode() {
        return debtController.debtState() != null;
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void rollDice() {
        gameTurnFlowCoordinator.rollDice();
    }

    private void runComputerPlayerStep() {
        botTurnDriver.step(new GameBotTurnHooks());
    }

    private Player findPlayerById(String playerId) {
        if (playerId == null) {
            return null;
        }
        for (Player player : players.getPlayers()) {
            if (playerId.equals("player-" + player.getId())) {
                return player;
            }
        }
        return null;
    }

    private String resolveTradeActorId(SessionState sessionState) {
        if (sessionState.tradeState() == null) {
            return null;
        }
        if (sessionState.tradeState().status() == TradeStatus.EDITING) {
            return sessionState.tradeState().editingPlayerId();
        }
        return sessionState.tradeState().decisionRequiredFromPlayerId();
    }

    private void scheduleNextComputerAction(BotTurnScheduler.DelayKind delayKind, int now) {
        botTurnScheduler.schedule(
                delayKind,
                now,
                botSpeedMode,
                players.getPlayers().stream().allMatch(Player::isComputerControlled)
        );
    }

    private final class GameBotTurnHooks implements GameBotTurnDriver.Hooks {
        @Override
        public void updateLogTurnContext() {
            Game.this.updateLogTurnContext();
        }

        @Override
        public boolean gameOver() {
            return gameOver;
        }

        @Override
        public boolean animationsRunning() {
            return animations.isRunning();
        }

        @Override
        public boolean paused() {
            return paused;
        }

        @Override
        public int now() {
            return runtime.app().millis();
        }

        @Override
        public void syncPresentationState() {
            Game.this.syncTransientPresentationState();
        }

        @Override
        public SessionState sessionState() {
            return sessionApplicationService.currentState();
        }

        @Override
        public Player currentTurnPlayer() {
            return players.getTurn();
        }

        @Override
        public Player findPlayerById(String playerId) {
            return Game.this.findPlayerById(playerId);
        }

        @Override
        public String resolveTradeActorId(SessionState sessionState) {
            return Game.this.resolveTradeActorId(sessionState);
        }

        @Override
        public boolean handleComputerTradeTurn(Player tradeActor) {
            return tradeController.handleComputerTradeTurn(tradeActor);
        }

        @Override
        public boolean popupVisible() {
            return runtime.popupService().isAnyVisible();
        }

        @Override
        public boolean finishAuctionResolution(FinishAuctionResolutionCommand command) {
            return sessionApplicationService.handle(command).accepted();
        }

        @Override
        public boolean resolveVisiblePopupFor(Player turnPlayer) {
            return runtime.popupService().resolveForComputer(turnPlayer.getComputerProfile());
        }

        @Override
        public boolean handleComputerAuctionAction(String actorPlayerId) {
            return sessionApplicationService.handleComputerAuctionAction(actorPlayerId).accepted();
        }

        @Override
        public ComputerTurnContext createTurnContext(Player turnPlayer) {
            return new SessionBackedComputerTurnContext(
                    turnPlayer,
                    sessionApplicationService,
                    runtime,
                    () -> createGameView(turnPlayer),
                    () -> createPlayerView(turnPlayer),
                    () -> tradeController.tryInitiateComputerTrade(turnPlayer),
                    Game.this::syncTransientPresentationState,
                    Game.this::isProjectedRollDiceActionAvailable,
                    Game.this::isProjectedEndTurnActionAvailable
            );
        }

        @Override
        public BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context) {
            return ((SessionBackedComputerTurnContext) context).delayKind();
        }

        @Override
        public void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now) {
            scheduleNextComputerAction(delayKind, now);
        }

        @Override
        public void recordStep(long durationNanos) {
            debugPerformanceStats.recordComputerStep(durationNanos);
        }

        @Override
        public String sessionId() {
            return LOCAL_SESSION_ID;
        }
    }

    private void endRound(boolean switchTurns) {
        gameTurnFlowCoordinator.endRound(switchTurns);
    }

    public boolean onEvent(Event event) {
        updateLogTurnContext();
        return gameUiController.handleEvent(event);
    }

    private void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        updateLogTurnContext();
        sessionApplicationService.handlePaymentRequest(request, onResolved);
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
        return new GameSidebarPresenter.SidebarState(
                players.getTurn(),
                resolveCurrentTurnPhase(),
                players.getPlayers(),
                runtime.popupService().recentPopupMessages(),
                debtController.debtState(),
                getSidebarHistoryPanelY(),
                getSidebarHistoryHeight(),
                getSidebarReservedTop()
        );
    }

    private float getSidebarContentTop() {
        return gameSidebarPresenter.contentTop(getLayoutMetrics(), createSidebarState());
    }

    private void updateDebtButtons() {
        DebtState debtState = debtController.debtState();
        var activeDebt = sessionApplicationService != null ? sessionApplicationService.currentState().activeDebt() : null;
        if (debtState == null || activeDebt == null) {
            retryDebtButton.hide();
            declareBankruptcyButton.hide();
            return;
        }
        retryDebtButton.show();
        if (activeDebt.bankruptcyRisk()) {
            declareBankruptcyButton.show();
        } else {
            declareBankruptcyButton.hide();
        }
    }

    private void updateDebugButtons() {
        gameUiController.updatePersistentButtons(gameOver);
    }

    private void refreshLabels() {
        gameUiController.refreshLabels(paused, botSpeedMode);
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
        if (gameOver) {
            hidePrimaryTurnControls();
            return;
        }
        primaryTurnControlState = PrimaryTurnControlState.ROLL_DICE;
        dices.reset();
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            dices.hide();
            endRoundButton.hide();
            return;
        }
        dices.show();
        endRoundButton.hide();
    }

    private void showEndTurnControl() {
        if (gameOver) {
            hidePrimaryTurnControls();
            return;
        }
        primaryTurnControlState = PrimaryTurnControlState.END_TURN;
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            dices.hide();
            endRoundButton.hide();
            return;
        }
        dices.hide();
        endRoundButton.show();
    }

    private void hidePrimaryTurnControls() {
        primaryTurnControlState = PrimaryTurnControlState.NONE;
        dices.hide();
        endRoundButton.hide();
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
        if (gameOver && winner != null) {
            MDC.put("turnPlayer", winner.getName());
            return;
        }
        Player turnPlayer = players != null ? players.getTurn() : null;
        MDC.put("turnPlayer", turnPlayer != null ? turnPlayer.getName() : "none");
    }

    private void syncTransientPresentationState() {
        auctionViewAdapter.sync();
        tradeViewAdapter.sync();
    }

    private void enforcePrimaryTurnControlInvariant() {
        if (debtController.debtState() != null) {
            hidePrimaryTurnControls();
            return;
        }
        if (endRoundButton.isVisible() && dices.isVisible()) {
            log.warn("Primary turn controls were both visible. Hiding roll dice button to keep end-turn state authoritative.");
            dices.hide();
        }
    }

    private final class GameUiHooks implements GameUiController.Hooks {
        @Override
        public boolean gameOver() {
            return gameOver;
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
        public boolean canEndTurn() {
            return endRoundButton.isVisible();
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
        public void openTradeMenu() {
            tradeController.openTradeMenu();
        }

        @Override
        public void payDebt() {
            debtActionDispatcher.payDebt();
        }

        @Override
        public void declareBankruptcy() {
            debtActionDispatcher.declareBankruptcy();
        }

        @Override
        public void endRound() {
            if (!runtime.popupService().isAnyVisible() && debtController.debtState() == null) {
                gameTurnFlowCoordinator.endRound(true);
            }
        }

        @Override
        public void openGodModeMenu() {
            debugController.openGodModeMenu();
        }

        @Override
        public void finishAllAnimations() {
            animations.finishAllAnimations();
        }

        @Override
        public void toggleSkipAnimations() {
            MonopolyApp.SKIP_ANNIMATIONS = !MonopolyApp.SKIP_ANNIMATIONS;
            log.debug("Skip animations: {}", MonopolyApp.SKIP_ANNIMATIONS);
        }

        @Override
        public Spot hoveredSpot() {
            return board.getHoveredSpot();
        }

        @Override
        public boolean debugFlyToHoveredSpot(Spot hoveredSpot) {
            if (!dices.isVisible()) {
                return false;
            }
            dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
            boolean played = gameTurnFlowCoordinator.playDebugRound(hoveredSpot, DiceState.DEBUG_REROLL);
            if (played) {
                dices.hide();
            }
            return played;
        }

        @Override
        public Locale currentLocale() {
            return fi.monopoly.text.UiTexts.getLocale();
        }

        @Override
        public void switchLanguage(Locale locale) {
            Game.this.switchLanguage(locale);
        }
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
        if (currentPlayer == null || runtime.popupService().isAnyVisible() || debtController.debtState() != null) {
            return false;
        }
        return primaryTurnControlState == PrimaryTurnControlState.ROLL_DICE;
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        if (currentPlayer == null || runtime.popupService().isAnyVisible() || debtController.debtState() != null) {
            return false;
        }
        return primaryTurnControlState == PrimaryTurnControlState.END_TURN;
    }

    private boolean isProjectedRollDiceActionAvailable() {
        return isRollDiceActionAvailable(players != null ? players.getTurn() : null);
    }

    private boolean isProjectedEndTurnActionAvailable() {
        return isEndTurnActionAvailable(players != null ? players.getTurn() : null);
    }

    private enum PrimaryTurnControlState {
        NONE,
        ROLL_DICE,
        END_TURN
    }

    private final class GameTurnFlowHooks implements GameTurnFlowCoordinator.Hooks {
        @Override
        public void updateLogTurnContext() {
            Game.this.updateLogTurnContext();
        }

        @Override
        public boolean gameOver() {
            return gameOver;
        }

        @Override
        public boolean debtActive() {
            return debtController.debtState() != null;
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
        public void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
            Game.this.handlePaymentRequest(request, onResolved);
        }
    }

    private int estimateRent(Property property, Player owner) {
        if (property.getSpotType().streetType.placeType == PlaceType.UTILITY) {
            return switch (owner.countOwnedProperties(property.getSpotType().streetType)) {
                case 2 -> 70;
                default -> 28;
            };
        }
        Player nonOwner = null;
        for (Player candidate : players.getPlayers()) {
            if (candidate != owner) {
                nonOwner = candidate;
                break;
            }
        }
        return nonOwner == null ? 0 : property.getRent(nonOwner);
    }

    private int calculateBoardDangerScore(Player player) {
        int boardDangerScore = 0;
        for (Spot spot : board.getSpots()) {
            if (!(spot instanceof PropertySpot propertySpot)) {
                continue;
            }
            Property property = propertySpot.getProperty();
            if (!property.hasOwner() || !property.isNotOwner(player)) {
                continue;
            }
            boardDangerScore += calculateDangerRent(property);
        }
        return boardDangerScore;
    }

    private int calculateDangerRent(Property property) {
        Player owner = property.getOwnerPlayer();
        if (owner == null) {
            return 0;
        }
        return switch (property.getSpotType().streetType.placeType) {
            case UTILITY -> owner.countOwnedProperties(property.getSpotType().streetType) >= 2 ? 70 : 28;
            case RAILROAD, STREET -> estimateRent(property, owner);
            default -> 0;
        };
    }

    private int countUnownedProperties() {
        int unownedProperties = 0;
        for (Spot spot : board.getSpots()) {
            if (spot instanceof PropertySpot propertySpot && !propertySpot.getProperty().hasOwner()) {
                unownedProperties++;
            }
        }
        return unownedProperties;
    }

}
