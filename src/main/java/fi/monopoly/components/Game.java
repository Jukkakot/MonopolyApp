package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.computer.*;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.trade.TradeController;
import fi.monopoly.components.turn.*;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.session.LegacyPopupSnapshot;
import fi.monopoly.presentation.session.LegacySessionProjector;
import fi.monopoly.text.UiTexts;
import fi.monopoly.types.*;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.TextWrapUtils;
import fi.monopoly.utils.UiTokens;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import processing.core.PConstants;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.*;

import static fi.monopoly.text.UiTexts.text;
import static processing.event.MouseEvent.CLICK;

@Slf4j
public class Game implements MonopolyEventListener {
    private static final String LOCAL_SESSION_ID = "local-session";
    // UI and layout constants
    private static final List<Locale> SUPPORTED_UI_LOCALES = List.of(
            Locale.forLanguageTag("fi"),
            Locale.ENGLISH
    );
    private static final boolean FORCE_DEBT_DEBUG_SCENARIO = false;
    private static final int NO_COMPUTER_ACTION_YET = -1;

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

    private enum ComputerActionDelayKind {
        ANIMATION_FINISH,
        RESOLVE_POPUP,
        ACCEPT_POPUP,
        DECLINE_POPUP,
        ROLL_DICE,
        END_TURN,
        BUILD_ROUND,
        SELL_BUILDING,
        MORTGAGE_PROPERTY,
        UNMORTGAGE_PROPERTY,
        TRADE,
        RETRY_DEBT_PAYMENT,
        DECLARE_BANKRUPTCY
    }

    // Core services
    private final MonopolyRuntime runtime;
    private final TurnEngine turnEngine = new TurnEngine();
    private final SessionApplicationService sessionApplicationService;
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
    private TurnResult prevTurnResult;
    private int lastComputerActionAt = NO_COMPUTER_ACTION_YET;
    private int nextComputerActionReadyAt = NO_COMPUTER_ACTION_YET;
    private boolean paused;
    private boolean gameOver;
    private Player winner;
    private BotSpeedMode botSpeedMode = BotSpeedMode.NORMAL;
    private final DebugPerformanceStats debugPerformanceStats = new DebugPerformanceStats();
    private LayoutMetrics frameLayoutMetrics;
    private float frameSidebarHistoryHeight;
    private float frameSidebarHistoryPanelY;
    private float frameSidebarReservedTop;
    private int lastSidebarLayoutHash = Integer.MIN_VALUE;
    private long lastAnimationUpdateNanos = -1L;
    private final Map<HistoryEntryCacheKey, HistoryEntryLayout> historyLayoutCache = new HashMap<>();
    private final Map<Integer, CachedPlayerView> playerViewCache = new HashMap<>();
    private CachedGameView cachedGameView;

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
                        () -> dices != null && dices.isVisible(),
                        () -> endRoundButton.isVisible()
                )::project
        );
        registerGameSession();
        setupDefaultGameState();
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
                this::updateDebtButtons,
                this::declareWinner
        );
        tradeController = new TradeController(
                runtime,
                () -> !gameOver && !runtime.popupService().isAnyVisible() && debtController.debtState() == null,
                () -> players != null ? players.getTurn() : null,
                () -> players != null ? players.getPlayers() : List.of()
        );
        debugController = new DebugController(
                runtime,
                board,
                () -> players != null ? players.getTurn() : null,
                this::debugResetTurnState,
                this::restoreNormalTurnControls,
                debtController::retryPendingDebtPayment,
                this::handlePaymentRequest
        );
    }

    private void registerGameSession() {
        runtime.setGameSession(new GameSession(players, dices, animations)
                .withStateSuppliers(
                        () -> debtController != null && debtController.debtState() != null,
                        () -> gameOver,
                        () -> goMoneyAmount
                ));
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

    private void setupButtonActions() {
        endRoundButton.addListener(e -> {
            if (!runtime.popupService().isAnyVisible() && debtController.debtState() == null) {
                endRound(true);
            }
        });
        retryDebtButton.addListener(debtController::retryPendingDebtPayment);
        declareBankruptcyButton.addListener(debtController::declareBankruptcy);
        debugGodModeButton.addListener(debugController::openGodModeMenu);
        pauseButton.addListener(this::togglePause);
        tradeButton.addListener(tradeController::openTradeMenu);
        botSpeedButton.addListener(this::cycleBotSpeedMode);
        languageButton.addListener(this::toggleLanguage);
    }

    private enum BotSpeedMode {
        NORMAL("game.button.botSpeed.normal", 3.0f),
        FAST("game.button.botSpeed.fast", 1.0f),
        INSTANT("game.button.botSpeed.instant", 0.0f);

        private final String labelKey;
        private final float delayMultiplier;

        BotSpeedMode(String labelKey, float delayMultiplier) {
            this.labelKey = labelKey;
            this.delayMultiplier = delayMultiplier;
        }

        private BotSpeedMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
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
            drawSidebarPanel(layoutMetrics);
        }
        if (!isDebtSidebarMode()) {
            dices.draw(null);
        }
        if (hasSidebarSpace && isDebtSidebarMode()) {
            players.focusPlayer(debtController.debtState().paymentRequest().debtor());
        }
        if (hasSidebarSpace) {
            players.draw(getSidebarContentTop(), !isDebtSidebarMode(), !isDebtSidebarMode());
        } else {
            players.drawTokens();
        }
        updateDebugButtons();
        enforcePrimaryTurnControlInvariant();
        if (hasSidebarSpace) {
            drawDebtState();
        }
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
        frameLayoutMetrics = LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
        frameSidebarReservedTop = frameLayoutMetrics.sidebarReservedTop(MonopolyApp.DEBUG_MODE);
        float availableHistoryHeight = runtime.app().height - frameSidebarReservedTop - frameLayoutMetrics.sidebarHistoryBottomMargin() - UiTokens.sidebarHistoryTopMargin();
        frameSidebarHistoryHeight = Math.max(UiTokens.sidebarHistoryMinHeight(), Math.min(UiTokens.sidebarHistoryPreferredHeight(), availableHistoryHeight));
        frameSidebarHistoryPanelY = runtime.app().height - frameSidebarHistoryHeight - frameLayoutMetrics.sidebarHistoryBottomMargin();
        return frameLayoutMetrics;
    }

    private void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        if (MonopolyApp.SKIP_ANNIMATIONS || !animationWasRunning || animations.isRunning()) {
            return;
        }
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            scheduleNextComputerAction(ComputerActionDelayKind.ANIMATION_FINISH, runtime.app().millis());
        }
    }

    /**
     * Draws the persistent right-side information panel so turn state, player
     * overview and controls stay in one predictable area.
     */
    private void drawSidebarPanel(LayoutMetrics layoutMetrics) {
        float sidebarX = layoutMetrics.sidebarX();
        float sidebarWidth = layoutMetrics.sidebarWidth();
        if (!layoutMetrics.hasSidebarSpace()) {
            return;
        }
        MonopolyApp app = runtime.app();
        app.push();
        app.noStroke();
        app.fill(245, 239, 221);
        app.rect(sidebarX, 0, sidebarWidth, app.height);

        app.stroke(193, 178, 140);
        app.strokeWeight(2);
        app.line(sidebarX, 0, sidebarX, app.height);
        app.line(sidebarX + UiTokens.spacingMd(), layoutMetrics.sidebarHeaderHeight(), sidebarX + sidebarWidth - UiTokens.spacingMd(), layoutMetrics.sidebarHeaderHeight());
        if (isDebtSidebarMode()) {
            app.line(sidebarX + UiTokens.spacingMd(), getDebtSectionBottom(), sidebarX + sidebarWidth - UiTokens.spacingMd(), getDebtSectionBottom());
        }

        Player turnPlayer = players.getTurn();
        app.fill(46, 72, 63);
        app.textAlign(PConstants.LEFT);
        app.textFont(runtime.font30());
        app.text(text("sidebar.title"), sidebarX + UiTokens.spacingMd(), layoutMetrics.sidebarTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.text(text("sidebar.currentPlayer"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow1Y());
        app.text(text("sidebar.turnPhase"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow2Y());
        app.text(text("sidebar.currentSpot"), sidebarX + UiTokens.sidebarLabelX(), layoutMetrics.sidebarHeaderRow3Y());

        app.fill(46, 72, 63);
        float currentPlayerValueX = sidebarX + UiTokens.sidebarValueX();
        float currentPlayerValueY = layoutMetrics.sidebarHeaderRow1Y();
        app.text(turnPlayer != null ? turnPlayer.getName() : text("sidebar.none"), currentPlayerValueX, currentPlayerValueY);
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            drawComputerBadge(app, currentPlayerValueX + app.textWidth(turnPlayer.getName()) + 12, currentPlayerValueY - 14);
        }
        app.text(resolveCurrentTurnPhase(), sidebarX + UiTokens.sidebarValueX(), layoutMetrics.sidebarHeaderRow2Y());
        app.text(turnPlayer != null && turnPlayer.getSpot() != null ? turnPlayer.getSpot().getName() : text("sidebar.none"), sidebarX + UiTokens.sidebarValueX(), layoutMetrics.sidebarHeaderRow3Y());
        drawPopupHistoryPanel(app, layoutMetrics);
        app.pop();
    }

    private void drawComputerBadge(MonopolyApp app, float x, float y) {
        app.pushStyle();
        app.rectMode(PConstants.CORNER);
        app.noStroke();
        app.fill(46, 72, 63);
        app.rect(x, y, 42, 18, 8);
        app.fill(255);
        app.textFont(runtime.font10());
        app.textAlign(PConstants.CENTER, PConstants.TOP);
        app.text("BOT", x + 21, y + 4);
        app.popStyle();
    }

    /**
     * Keeps the latest popup texts visible after accidental dismissals without
     * covering the game board itself.
     */
    private void drawPopupHistoryPanel(MonopolyApp app, LayoutMetrics layoutMetrics) {
        long historyStart = System.nanoTime();
        float panelX = layoutMetrics.sidebarX() + UiTokens.spacingMd();
        float historyHeight = getSidebarHistoryHeight();
        float panelY = getSidebarHistoryPanelY();
        float panelW = layoutMetrics.sidebarWidth() - UiTokens.spacingMd() * 2;
        List<String> recentMessages = runtime.popupService().recentPopupMessages();

        app.noStroke();
        app.fill(255, 249, 233);
        app.rect(panelX, panelY, panelW, historyHeight, 16);

        app.stroke(193, 178, 140);
        app.strokeWeight(2);
        app.line(panelX, panelY + UiTokens.sidebarHistoryHeaderHeight(), panelX + panelW, panelY + UiTokens.sidebarHistoryHeaderHeight());

        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.history"), panelX + UiTokens.sidebarHistoryTextInset(), panelY + 24);

        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT, PConstants.TOP);

        if (recentMessages.isEmpty()) {
            app.text(
                    text("sidebar.history.empty"),
                    panelX + UiTokens.sidebarHistoryTextInset(),
                    panelY + 56,
                    panelW - UiTokens.sidebarHistoryTextInset() * 2,
                    historyHeight - 48
            );
            return;
        }

        float maxTextWidth = panelW - UiTokens.sidebarHistoryTextInset() * 2;
        float currentBottomY = panelY + historyHeight - 20;
        for (String message : recentMessages) {
            HistoryEntryLayout layout = buildHistoryEntryLayout(app, message, maxTextWidth);
            if (layout == null) {
                continue;
            }
            float nextTopY = currentBottomY - layout.height();
            if (nextTopY < panelY + UiTokens.sidebarHistoryHeaderHeight() + 8) {
                break;
            }
            drawHistoryEntry(app, layout, panelX + UiTokens.sidebarHistoryTextInset(), nextTopY);
            currentBottomY = nextTopY - 8;
        }
        debugPerformanceStats.recordHistoryLayout(System.nanoTime() - historyStart);
    }

    private HistoryEntryLayout buildHistoryEntryLayout(MonopolyApp app, String message, float maxTextWidth) {
        String condensedMessage = condenseHistoryMessage(message);
        if (condensedMessage.isEmpty()) {
            return null;
        }
        HistoryEntryCacheKey cacheKey = new HistoryEntryCacheKey(condensedMessage, Math.round(maxTextWidth));
        HistoryEntryLayout cachedLayout = historyLayoutCache.get(cacheKey);
        if (cachedLayout != null) {
            return cachedLayout;
        }
        int separatorIndex = condensedMessage.indexOf(": ");
        if (separatorIndex <= 0) {
            List<String> lines = TextWrapUtils.wrapText(app.g, "- " + condensedMessage, maxTextWidth, "history.generic");
            HistoryEntryLayout layout = new HistoryEntryLayout(null, null, lines, lines.size() * 22f);
            historyLayoutCache.put(cacheKey, layout);
            return layout;
        }

        String playerName = condensedMessage.substring(0, separatorIndex).trim();
        String body = condensedMessage.substring(separatorIndex + 2).trim();
        Player messagePlayer = findPlayerByName(playerName);
        if (messagePlayer == null) {
            List<String> lines = TextWrapUtils.wrapText(app.g, "- " + condensedMessage, maxTextWidth, "history.generic");
            HistoryEntryLayout layout = new HistoryEntryLayout(null, null, lines, lines.size() * 22f);
            historyLayoutCache.put(cacheKey, layout);
            return layout;
        }

        String prefix = "- " + playerName + ":";
        float prefixWidth = app.textWidth(prefix + " ");
        List<String> wrappedBodyLines = TextWrapUtils.wrapText(app.g, body, Math.max(40, maxTextWidth - prefixWidth), "history.player");
        if (wrappedBodyLines.isEmpty()) {
            wrappedBodyLines = List.of("");
        }
        HistoryEntryLayout layout = new HistoryEntryLayout(messagePlayer, prefix, wrappedBodyLines, wrappedBodyLines.size() * 22f);
        historyLayoutCache.put(cacheKey, layout);
        return layout;
    }

    private void drawHistoryEntry(MonopolyApp app, HistoryEntryLayout layout, float startX, float startY) {
        if (layout.player() == null || layout.prefix() == null) {
            drawWrappedHistoryLines(app, layout.lines(), startX, startY);
            return;
        }
        float prefixWidth = app.textWidth(layout.prefix() + " ");
        app.fill(colorComponent(layout.player().getColor().getRed()),
                colorComponent(layout.player().getColor().getGreen()),
                colorComponent(layout.player().getColor().getBlue()));
        app.text(layout.prefix(), startX, startY);

        app.fill(0);
        app.text(layout.lines().get(0), startX + prefixWidth, startY);
        float currentY = startY;
        for (int i = 1; i < layout.lines().size(); i++) {
            currentY += 22;
            app.text(layout.lines().get(i), startX + 18, currentY);
        }
    }

    private void drawWrappedHistoryLines(MonopolyApp app, List<String> wrappedLines, float startX, float startY) {
        float currentY = startY;
        for (String line : wrappedLines) {
            app.text(line, startX, currentY);
            currentY += 22;
        }
    }

    private String condenseHistoryMessage(String message) {
        return message.replaceAll("\\R+", " / ").replaceAll("\\s{2,}", " ").trim();
    }

    private Player findPlayerByName(String playerName) {
        if (players == null || playerName == null || playerName.isBlank()) {
            return null;
        }
        for (Player player : players.getPlayers()) {
            if (player.getName().equals(playerName)) {
                return player;
            }
        }
        return null;
    }

    private int colorComponent(double component) {
        return (int) Math.round(component * 255);
    }

    private record HistoryEntryLayout(
            Player player,
            String prefix,
            List<String> lines,
            float height
    ) {
    }

    private record HistoryEntryCacheKey(
            String message,
            int width
    ) {
    }

    private record CachedPlayerView(
            long signature,
            PlayerView view
    ) {
    }

    private record CachedGameView(
            long signature,
            GameView view
    ) {
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

    private float getSidebarContentTop() {
        if (debtController.debtState() != null) {
            return getDebtSectionBottom() + 20;
        }
        float debugFloor = getSidebarReservedTop();
        float historyTop = getSidebarHistoryPanelY();
        float availableTop = historyTop - UiTokens.sidebarHistoryTopMargin();
        return Math.max(UiTokens.sidebarMinContentTop(), Math.min(debugFloor, availableTop));
    }

    private boolean isDebtSidebarMode() {
        return debtController.debtState() != null;
    }

    private String buildDebtSidebarText(PaymentRequest request) {
        return text(
                "sidebar.debt.summary",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.debtor().getMoneyAmount(),
                request.debtor().getTotalLiquidationValue()
        );
    }

    private int getDebtSectionBottom() {
        DebtState debtState = debtController.debtState();
        if (debtState == null) {
            return Math.round(getLayoutMetrics().debtTextY());
        }
        int lineCount = buildDebtSidebarText(debtState.paymentRequest()).split("\\R").length;
        return Math.round(getLayoutMetrics().debtTextY() + lineCount * UiTokens.sidebarLineHeight());
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void rollDice() {
        updateLogTurnContext();
        if (runtime.popupService().isAnyVisible() || debtController.debtState() != null) {
            log.trace("Ignoring rollDice because popupVisible={} debtStateActive={}",
                    runtime.popupService().isAnyVisible(), debtController.debtState() != null);
            return;
        }
        log.debug("Starting turn roll for player {}", players.getTurn().getName());
        playRound(dices.getValue());
    }

    private void runComputerPlayerStep() {
        long stepStart = System.nanoTime();
        updateLogTurnContext();
        if (gameOver) {
            return;
        }
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null || !turnPlayer.isComputerControlled()) {
            return;
        }
        if (animations.isRunning()) {
            return;
        }
        if (paused) {
            return;
        }
        int now = runtime.app().millis();
        boolean shouldApplyDelay = !MonopolyApp.SKIP_ANNIMATIONS && nextComputerActionReadyAt != NO_COMPUTER_ACTION_YET;
        if (shouldApplyDelay && now < nextComputerActionReadyAt) {
            return;
        }

        GameComputerTurnContext context = new GameComputerTurnContext(turnPlayer);
        boolean acted = ComputerStrategies.forProfile(turnPlayer.getComputerProfile()).takeStep(context);
        if (acted) {
            scheduleNextComputerAction(context.delayKind(), now);
        }
        debugPerformanceStats.recordComputerStep(System.nanoTime() - stepStart);
    }

    private void scheduleNextComputerAction(ComputerActionDelayKind delayKind, int now) {
        lastComputerActionAt = now;
        nextComputerActionReadyAt = now + computeComputerActionDelayMs(delayKind);
    }

    private int computeComputerActionDelayMs(ComputerActionDelayKind delayKind) {
        if (MonopolyApp.SKIP_ANNIMATIONS || botSpeedMode == BotSpeedMode.INSTANT) {
            return 0;
        }
        int baseDelayMs = switch (delayKind) {
            case ANIMATION_FINISH -> 260;
            case RESOLVE_POPUP, ACCEPT_POPUP, DECLINE_POPUP -> 220;
            case ROLL_DICE -> 240;
            case END_TURN -> 150;
            case BUILD_ROUND -> 700;
            case SELL_BUILDING -> 520;
            case MORTGAGE_PROPERTY, UNMORTGAGE_PROPERTY -> 480;
            case TRADE -> 850;
            case RETRY_DEBT_PAYMENT, DECLARE_BANKRUPTCY -> 650;
        };
        float multiplier = botSpeedMode.delayMultiplier;
        if (players.getPlayers().stream().allMatch(Player::isComputerControlled)) {
            multiplier *= 0.7f;
        }
        int jitter = (int) ((Math.random() * 120) - 60);
        return Math.max(0, Math.round(baseDelayMs * multiplier) + jitter);
    }

    private void endRound(boolean switchTurns) {
        updateLogTurnContext();
        if (gameOver) {
            hidePrimaryTurnControls();
            return;
        }
        Player currentTurn = players.getTurn();
        prevTurnResult = null;
        if (switchTurns) {
            players.switchTurn();
        }
        updateLogTurnContext();
        showRollDiceControl();
        log.debug("Ending round. previousTurnPlayer={}, switchTurns={}, nextTurnPlayer={}",
                currentTurn != null ? currentTurn.getName() : "none",
                switchTurns,
                players.getTurn() != null ? players.getTurn().getName() : "none");
    }

    private void playRound(DiceValue diceValue) {
        Player turnPlayer = players.getTurn();
        log.debug("Playing round for player {} with diceState={} value={}",
                turnPlayer.getName(), diceValue.diceState(), diceValue.value());
        if (turnPlayer.isInJail()) {
            ((JailSpot) turnPlayer.getSpot()).handleInJailTurn(
                    turnPlayer,
                    diceValue,
                    this::handlePaymentRequest,
                    () -> applyTurnPlan(turnEngine.endTurn(false)),
                    () -> applyTurnPlan(turnEngine.endTurn(true))
            );
            return;
        }
        if (DiceState.JAIL.equals(diceValue.diceState())) {
            prevTurnResult = TurnResult.builder().shouldGoToJail(true).build();
        }
        applyTurnPlan(turnEngine.createMovementPlan(turnPlayer, board, diceValue));
    }

    private void addAnimationAndHandleSpot(Path path, CallbackAction onAnimationEnd) {
        PlayerToken turnPlayer = players.getTurn();
        animations.addAnimation(new Animation(turnPlayer, path, onAnimationEnd));
    }

    private boolean playRound(Spot newSpot, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        if (newSpot.equals(turnPlayer.getSpot())) {
            runtime.popupService().show(text("game.move.sameSpot"));
            return false;
        }
        if (turnPlayer.isInJail()) {
            runtime.popupService().show(text("game.move.inJail"));
            return false;
        }
        PathMode pathMode = DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState) ? PathMode.FLY : PathMode.NORMAL;
        Path path = board.getPath(turnPlayer, newSpot, pathMode);
        return playRound(path, diceState);
    }

    private boolean playRound(Path path, DiceState diceState) {
        Player turnPlayer = players.getTurn();
        log.trace("Animating player {} along path to {} with diceState={}",
                turnPlayer.getName(), path.getLastSpot().getSpotType(), diceState);
        addAnimationAndHandleSpot(path, () -> {
            CallbackAction completeTurnMove = () -> {
                turnPlayer.setSpot(path.getLastSpot());
                log.trace("Player {} arrived at {}", turnPlayer.getName(), path.getLastSpot().getSpotType());
                handleSpotLogic(diceState, path.getLastSpot());
            };
            if (path.passesGoSpot()) {
                log.info("Player {} passed GO", turnPlayer.getName());
                runtime.popupService().show(text("game.go.reward", goMoneyAmount), () -> {
                    turnPlayer.addMoney(goMoneyAmount);
                    completeTurnMove.doAction();
                });
                return;
            }
            completeTurnMove.doAction();
        });
        return true;
    }

    private void handleSpotLogic(DiceState diceState, Spot spot) {
        log.debug("Handling spot logic for player {} on spot {} with diceState={}",
                players.getTurn().getName(), spot.getSpotType(), diceState);
        GameState gameState = new GameState(players, dices, board, TurnResult.copyOf(prevTurnResult), this::handlePaymentRequest);
        prevTurnResult = null; //Important to clear previous turn result before getting next one!
        prevTurnResult = spot.handleTurn(gameState, () -> doTurnEndEvent(diceState));
        log.trace("Spot handling produced prevTurnResult={}", prevTurnResult);
    }

    private void doTurnEndEvent(DiceState diceState) {
        log.trace("Running turn end event for player {} with diceState={} prevTurnResult={}",
                players.getTurn().getName(), diceState, prevTurnResult);
        applyTurnPlan(turnEngine.createFollowUpPlan(players.getTurn(), board, prevTurnResult, diceState));
    }

    private void applyTurnPlan(TurnPlan turnPlan) {
        log.debug("Applying turn plan phase={} effectCount={}", turnPlan.phase(), turnPlan.effects().size());
        for (TurnEffect effect : turnPlan.effects()) {
            log.trace("Applying turn effect {}", effect.getClass().getSimpleName());
            if (effect instanceof MovePlayerEffect movePlayerEffect) {
                playRound(movePlayerEffect.path(), movePlayerEffect.diceState());
            } else if (effect instanceof ShowDiceEffect) {
                showRollDiceControl();
            } else if (effect instanceof ShowEndTurnEffect) {
                showEndTurnControl();
            } else if (effect instanceof EndTurnEffect endTurnEffect) {
                endRound(endTurnEffect.switchTurns());
            } else {
                throw new IllegalStateException("Unhandled turn effect: " + effect.getClass().getSimpleName());
            }
        }
    }

    public boolean onEvent(Event event) {
        updateLogTurnContext();
        boolean consumedEvent = false;
        if (event instanceof KeyEvent keyEvent) {
            char key = Character.toLowerCase(keyEvent.getKey());
            if (gameOver) {
                return false;
            }
            if (key == 'p') {
                togglePause();
                return true;
            }
            if (key == 'b') {
                cycleBotSpeedMode();
                return true;
            }
            if (runtime.popupService().isAnyVisible()) {
                return consumedEvent;
            }
            if (debtController.debtState() != null) {
                if (key == MonopolyApp.SPACE || key == MonopolyApp.ENTER) {
                    debtController.retryPendingDebtPayment();
                    return true;
                }
                return false;
            }
            if (key == 't') {
                tradeController.openTradeMenu();
                consumedEvent = true;
            }
            if (endRoundButton.isVisible() && (key == MonopolyApp.SPACE || key == MonopolyApp.ENTER)) {
                endRound(true);
                consumedEvent = true;
            }
            if (MonopolyApp.DEBUG_MODE && key == 'e') {
                log.debug("Ending round");
                animations.finishAllAnimations();
                endRound(true);
                consumedEvent = true;
            }
            if (MonopolyApp.DEBUG_MODE && key == 'g') {
                debugController.openGodModeMenu();
                consumedEvent = true;
            }
            if (key == 'a') {
                MonopolyApp.SKIP_ANNIMATIONS = !MonopolyApp.SKIP_ANNIMATIONS;
                log.debug("Skip animations: {}", MonopolyApp.SKIP_ANNIMATIONS);
                consumedEvent = true;
            }
        } else if (event instanceof MouseEvent mouseEvent) {
            if (mouseEvent.getAction() == CLICK) {
                if (runtime.popupService().isAnyVisible()) {
                    return consumedEvent;
                }
                Spot hoveredSpot = board.getHoveredSpot();
                //Debugging "flying mechanic"
                if (hoveredSpot != null && MonopolyApp.DEBUG_MODE && dices.isVisible()) {
                    dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
                    if (playRound(hoveredSpot, DiceState.DEBUG_REROLL)) {
                        consumedEvent = true;
                        dices.hide();
                    }
                }
            }
        }
        return consumedEvent;
    }

    private void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        updateLogTurnContext();
        debtController.handlePaymentRequest(request, onResolved);
    }

    private void drawDebtState() {
        DebtState debtState = debtController.debtState();
        if (debtState == null) {
            return;
        }

        LayoutMetrics layoutMetrics = getLayoutMetrics();
        MonopolyApp app = runtime.app();
        app.push();
        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.debt"), layoutMetrics.sidebarX() + UiTokens.spacingMd(), layoutMetrics.debtSectionTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT);
        PaymentRequest request = debtState.paymentRequest();
        app.text(
                buildDebtSidebarText(request),
                layoutMetrics.sidebarX() + UiTokens.spacingMd(),
                layoutMetrics.debtTextY()
        );
        app.pop();
    }

    private LayoutMetrics getLayoutMetrics() {
        return frameLayoutMetrics != null ? frameLayoutMetrics : LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
    }

    private void updateSidebarControlPositions() {
        updateSidebarControlPositions(updateFrameLayoutMetrics());
    }

    private void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        int layoutHash = java.util.Objects.hash(
                layoutMetrics.hasSidebarSpace(),
                Math.round(layoutMetrics.sidebarX()),
                Math.round(layoutMetrics.sidebarRight()),
                Math.round(layoutMetrics.sidebarPrimaryButtonY()),
                Math.round(layoutMetrics.sidebarDebugButtonRow1Y()),
                Math.round(frameSidebarHistoryHeight),
                Math.round(frameSidebarHistoryPanelY),
                MonopolyApp.DEBUG_MODE,
                pauseButton.getWidth(),
                tradeButton.getWidth(),
                botSpeedButton.getWidth(),
                languageButton.getWidth(),
                declareBankruptcyButton.getWidth()
        );
        if (layoutHash == lastSidebarLayoutHash) {
            dices.updateLayout(layoutMetrics);
            return;
        }
        lastSidebarLayoutHash = layoutHash;
        if (!layoutMetrics.hasSidebarSpace()) {
            layoutOverlayControls(layoutMetrics);
            dices.updateLayout(layoutMetrics);
            return;
        }

        float sidebarLeftX = layoutMetrics.sidebarX() + UiTokens.spacingMd();
        float sidebarRightAlignedX = layoutMetrics.sidebarRight() - UiTokens.spacingMd();
        float primaryButtonY = layoutMetrics.sidebarPrimaryButtonY();
        float debugRow1Y = layoutMetrics.sidebarDebugButtonRow1Y();
        float controlRow1Y = frameSidebarHistoryPanelY + frameSidebarHistoryHeight + UiTokens.spacingSm();
        boolean showBotSpeed = MonopolyApp.DEBUG_MODE;
        float controlRow2Y = controlRow1Y + pauseButton.getHeight() + UiTokens.spacingXs();
        float lowestControlBottom = (showBotSpeed ? controlRow2Y : controlRow1Y) + languageButton.getHeight();
        if (lowestControlBottom > runtime.app().height - UiTokens.spacingMd()) {
            layoutOverlayControls(layoutMetrics);
            dices.updateLayout(layoutMetrics);
            return;
        }

        endRoundButton.setPosition(sidebarLeftX, primaryButtonY);
        retryDebtButton.setPosition(sidebarLeftX, primaryButtonY);
        declareBankruptcyButton.setPosition(sidebarRightAlignedX - declareBankruptcyButton.getWidth(), primaryButtonY);
        debugGodModeButton.setPosition(sidebarLeftX, debugRow1Y);
        if (showBotSpeed) {
            pauseButton.setPosition(sidebarRightAlignedX - pauseButton.getWidth(), controlRow1Y);
            tradeButton.setPosition(pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
            languageButton.setPosition(sidebarRightAlignedX - languageButton.getWidth(), controlRow2Y);
            botSpeedButton.setPosition(languageButton.getPosition()[0] - botSpeedButton.getWidth() - UiTokens.spacingXs(), controlRow2Y);
        } else {
            languageButton.setPosition(sidebarRightAlignedX - languageButton.getWidth(), controlRow1Y);
            pauseButton.setPosition(languageButton.getPosition()[0] - pauseButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
            tradeButton.setPosition(pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs(), controlRow1Y);
        }
        dices.updateLayout(layoutMetrics);
    }

    private void layoutOverlayControls(LayoutMetrics layoutMetrics) {
        float leftX = UiTokens.overlayMargin();
        float rightX = layoutMetrics.boardWidth() - UiTokens.overlayMargin();
        float overlayTopRowY = UiTokens.overlaySecondaryRow3Y();
        boolean showBotSpeed = MonopolyApp.DEBUG_MODE;
        float overlayBottomRowY = Math.min(
                overlayTopRowY + languageButton.getHeight() + UiTokens.spacingXs(),
                runtime.app().height - languageButton.getHeight() - UiTokens.spacingMd()
        );

        endRoundButton.setPosition(leftX, UiTokens.overlayPrimaryButtonY());
        retryDebtButton.setPosition(leftX, UiTokens.overlayPrimaryButtonY());
        declareBankruptcyButton.setPosition(rightX - declareBankruptcyButton.getWidth(), UiTokens.overlayPrimaryButtonY());
        debugGodModeButton.setPosition(leftX, UiTokens.overlaySecondaryRow1Y());
        if (showBotSpeed) {
            languageButton.setPosition(
                    Math.max(leftX, rightX - languageButton.getWidth()),
                    overlayBottomRowY
            );
            botSpeedButton.setPosition(
                    Math.max(leftX, languageButton.getPosition()[0] - botSpeedButton.getWidth() - UiTokens.spacingXs()),
                    overlayBottomRowY
            );
            pauseButton.setPosition(
                    Math.max(leftX, rightX - pauseButton.getWidth()),
                    overlayTopRowY
            );
            tradeButton.setPosition(
                    Math.max(leftX, pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
        } else {
            languageButton.setPosition(
                    Math.max(leftX, rightX - languageButton.getWidth()),
                    overlayTopRowY
            );
            pauseButton.setPosition(
                    Math.max(leftX, languageButton.getPosition()[0] - pauseButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
            tradeButton.setPosition(
                    Math.max(leftX, pauseButton.getPosition()[0] - tradeButton.getWidth() - UiTokens.spacingXs()),
                    overlayTopRowY
            );
        }
    }

    private float getSidebarHistoryHeight() {
        return frameSidebarHistoryHeight > 0 ? frameSidebarHistoryHeight : Math.max(
                UiTokens.sidebarHistoryMinHeight(),
                Math.min(
                        UiTokens.sidebarHistoryPreferredHeight(),
                        runtime.app().height - getSidebarReservedTop() - getLayoutMetrics().sidebarHistoryBottomMargin() - UiTokens.sidebarHistoryTopMargin()
                )
        );
    }

    private float getSidebarHistoryPanelY() {
        return frameSidebarHistoryPanelY > 0 ? frameSidebarHistoryPanelY : runtime.app().height - getSidebarHistoryHeight() - getLayoutMetrics().sidebarHistoryBottomMargin();
    }

    private float getSidebarReservedTop() {
        return frameSidebarReservedTop > 0 ? frameSidebarReservedTop : getLayoutMetrics().sidebarReservedTop(MonopolyApp.DEBUG_MODE);
    }

    private void updateDebtButtons() {
        DebtState debtState = debtController.debtState();
        if (debtState == null) {
            retryDebtButton.hide();
            declareBankruptcyButton.hide();
            return;
        }
        retryDebtButton.show();
        if (debtState.bankruptcyRisk()) {
            declareBankruptcyButton.show();
        } else {
            declareBankruptcyButton.hide();
        }
    }

    private void updateDebugButtons() {
        if (gameOver) {
            debugGodModeButton.hide();
            pauseButton.hide();
            tradeButton.hide();
            botSpeedButton.hide();
            languageButton.show();
            return;
        }
        pauseButton.show();
        tradeButton.show();
        languageButton.show();
        if (!MonopolyApp.DEBUG_MODE) {
            debugGodModeButton.hide();
            botSpeedButton.hide();
            return;
        }
        debugGodModeButton.show();
        botSpeedButton.show();
    }

    private void refreshLabels() {
        endRoundButton.setLabel(text("game.button.endRound"));
        retryDebtButton.setLabel(text("game.button.retryDebt"));
        declareBankruptcyButton.setLabel(text("game.button.bankrupt"));
        debugGodModeButton.setLabel(text("game.button.godMode"));
        pauseButton.setLabel(paused ? text("game.button.resume") : text("game.button.pause"));
        tradeButton.setLabel(text("game.button.trade"));
        botSpeedButton.setLabel(text("game.button.botSpeed", text(botSpeedMode.labelKey)));
        languageButton.setLabel(text("language.button.current", text("language.name.current")));
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
        nextComputerActionReadyAt = runtime.app().millis();
        refreshLabels();
        log.info("Bot speed mode={}", botSpeedMode);
    }

    private void toggleLanguage() {
        Locale currentLocale = fi.monopoly.text.UiTexts.getLocale();
        int currentIndex = SUPPORTED_UI_LOCALES.indexOf(currentLocale);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % SUPPORTED_UI_LOCALES.size();
        switchLanguage(SUPPORTED_UI_LOCALES.get(nextIndex));
    }

    private void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    private void debugResetTurnState() {
        log.debug("Debug action: reset turn state");
        animations.finishAllAnimations();
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
        dices.reset();
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            hidePrimaryTurnControls();
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
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            hidePrimaryTurnControls();
            return;
        }
        dices.hide();
        endRoundButton.show();
    }

    private void hidePrimaryTurnControls() {
        dices.hide();
        endRoundButton.hide();
    }

    private void declareWinner(Player winningPlayer) {
        gameOver = true;
        winner = winningPlayer;
        paused = false;
        prevTurnResult = null;
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

    private final class GameComputerTurnContext implements ComputerTurnContext {
        private final Player player;
        private ComputerActionDelayKind delayKind = ComputerActionDelayKind.RESOLVE_POPUP;

        private GameComputerTurnContext(Player player) {
            this.player = player;
        }

        private ComputerActionDelayKind delayKind() {
            return delayKind;
        }

        @Override
        public GameView gameView() {
            return createGameView(player);
        }

        @Override
        public PlayerView currentPlayerView() {
            return createPlayerView(player);
        }

        @Override
        public boolean resolveActivePopup() {
            boolean resolved = runtime.popupService().resolveForComputer(player.getComputerProfile());
            if (resolved) {
                delayKind = ComputerActionDelayKind.RESOLVE_POPUP;
            }
            return resolved;
        }

        @Override
        public boolean acceptActivePopup() {
            boolean accepted = runtime.popupService().triggerPrimaryComputerAction();
            if (accepted) {
                delayKind = ComputerActionDelayKind.ACCEPT_POPUP;
            }
            return accepted;
        }

        @Override
        public boolean declineActivePopup() {
            boolean declined = runtime.popupService().triggerSecondaryComputerAction();
            if (declined) {
                delayKind = ComputerActionDelayKind.DECLINE_POPUP;
            }
            return declined;
        }

        @Override
        public boolean sellBuilding(SpotType spotType, int count) {
            Property property = PropertyFactory.getProperty(spotType);
            if (!(property instanceof StreetProperty streetProperty)) {
                return false;
            }
            boolean sold = streetProperty.sellHouses(count);
            if (sold) {
                delayKind = ComputerActionDelayKind.SELL_BUILDING;
            }
            return sold;
        }

        @Override
        public boolean buyBuildingRound(SpotType spotType) {
            Property property = PropertyFactory.getProperty(spotType);
            if (!(property instanceof StreetProperty streetProperty)) {
                return false;
            }
            boolean built = streetProperty.buyBuildingRoundsAcrossSet(1);
            if (built) {
                delayKind = ComputerActionDelayKind.BUILD_ROUND;
            }
            return built;
        }

        @Override
        public boolean toggleMortgage(SpotType spotType) {
            Property property = PropertyFactory.getProperty(spotType);
            boolean wasMortgaged = property.isMortgaged();
            boolean toggled = property.handleMortgaging();
            if (toggled) {
                delayKind = wasMortgaged
                        ? ComputerActionDelayKind.UNMORTGAGE_PROPERTY
                        : ComputerActionDelayKind.MORTGAGE_PROPERTY;
            }
            return toggled;
        }

        @Override
        public ComputerDecision initiateTrade() {
            ComputerDecision decision = tradeController.tryInitiateComputerTrade(player);
            if (decision != null) {
                delayKind = ComputerActionDelayKind.TRADE;
            }
            return decision;
        }

        @Override
        public void retryPendingDebtPayment() {
            delayKind = ComputerActionDelayKind.RETRY_DEBT_PAYMENT;
            debtController.retryPendingDebtPayment();
        }

        @Override
        public void declareBankruptcy() {
            delayKind = ComputerActionDelayKind.DECLARE_BANKRUPTCY;
            debtController.declareBankruptcy();
        }

        @Override
        public void rollDice() {
            delayKind = ComputerActionDelayKind.ROLL_DICE;
            dices.rollDice();
        }

        @Override
        public void endTurn() {
            delayKind = ComputerActionDelayKind.END_TURN;
            Game.this.endRound(true);
        }
    }

    GameView createGameView(Player currentPlayer) {
        long snapshotStart = System.nanoTime();
        long gameViewSignature = buildGameViewSignature(currentPlayer);
        if (cachedGameView != null && cachedGameView.signature() == gameViewSignature) {
            debugPerformanceStats.recordGameViewBuild(System.nanoTime() - snapshotStart);
            return cachedGameView.view();
        }
        PopupView popupView = runtime.popupService().isAnyVisible()
                ? new PopupView(
                runtime.popupService().activePopupKind(),
                runtime.popupService().activePopupMessage(),
                runtime.popupService().activePopupActions(),
                createPopupPropertyView(currentPlayer)
        )
                : null;
        DebtState debtState = debtController.debtState();
        DebtView debtView = debtState == null ? null : new DebtView(
                debtState.paymentRequest().amount(),
                debtState.paymentRequest().reason(),
                debtState.bankruptcyRisk(),
                debtState.paymentRequest().target().getClass().getSimpleName(),
                debtTargetName(debtState.paymentRequest())
        );
        List<Player> playerList = players.getPlayers();
        List<PlayerView> playerViews = playerList.stream()
                .map(this::createPlayerView)
                .sorted(Comparator.comparingInt(PlayerView::turnNumber))
                .toList();
        GameView view = new GameView(
                currentPlayer.getId(),
                playerViews,
                new VisibleActionsView(
                        runtime.popupService().isAnyVisible(),
                        retryDebtButton.isVisible(),
                        declareBankruptcyButton.isVisible(),
                        isRollDiceActionAvailable(currentPlayer),
                        isEndTurnActionAvailable(currentPlayer)
                ),
                popupView,
                debtView,
                countUnownedProperties(),
                StreetProperty.BANK_HOUSE_SUPPLY - players.getTotalHouseCount(),
                StreetProperty.BANK_HOTEL_SUPPLY - players.getTotalHotelCount()
        );
        cachedGameView = new CachedGameView(gameViewSignature, view);
        debugPerformanceStats.recordGameViewBuild(System.nanoTime() - snapshotStart);
        return view;
    }

    public List<String> debugPerformanceLines(float fps) {
        return debugPerformanceStats.overlayLines(fps, MonopolyApp.getColoredImageCopies());
    }

    PlayerView createPlayerView(Player player) {
        long playerSignature = buildPlayerViewSignature(player);
        CachedPlayerView cachedPlayerView = playerViewCache.get(player.getId());
        if (cachedPlayerView != null && cachedPlayerView.signature() == playerSignature) {
            return cachedPlayerView.view();
        }
        List<Property> ownedPlayerProperties = player.getOwnedProperties();
        List<PropertyView> ownedProperties = ownedPlayerProperties.stream()
                .map(property -> createPropertyView(player, property))
                .sorted(Comparator.comparing(property -> property.spotType().ordinal()))
                .toList();
        List<StreetType> completedSets = ownedPlayerProperties.stream()
                .map(Property::getSpotType)
                .map(spotType -> spotType.streetType)
                .distinct()
                .filter(player::ownsAllStreetProperties)
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
        PlayerView playerView = new PlayerView(
                player.getId(),
                player.getName(),
                player.getMoneyAmount(),
                player.getTurnNumber(),
                player.getComputerProfile(),
                player.getSpot().getSpotType(),
                player.isInJail(),
                JailSpot.jailTimeLeftMap.getOrDefault(player, 0),
                player.getGetOutOfJailCardCount(),
                player.getTotalHouseCount(),
                player.getTotalHotelCount(),
                player.getTotalLiquidationValue(),
                calculateBoardDangerScore(player),
                completedSets,
                ownedProperties
        );
        playerViewCache.put(player.getId(), new CachedPlayerView(playerSignature, playerView));
        return playerView;
    }

    private long buildGameViewSignature(Player currentPlayer) {
        long signature = currentPlayer == null ? 0 : currentPlayer.getId();
        signature = signature * 31 + Boolean.hashCode(runtime.popupService().isAnyVisible());
        if (runtime.popupService().isAnyVisible()) {
            signature = signature * 31 + String.valueOf(runtime.popupService().activePopupKind()).hashCode();
            signature = signature * 31 + String.valueOf(runtime.popupService().activePopupMessage()).hashCode();
            signature = signature * 31 + runtime.popupService().activePopupActions().hashCode();
        }
        signature = signature * 31 + Boolean.hashCode(retryDebtButton.isVisible());
        signature = signature * 31 + Boolean.hashCode(declareBankruptcyButton.isVisible());
        signature = signature * 31 + Boolean.hashCode(isRollDiceActionAvailable(currentPlayer));
        signature = signature * 31 + Boolean.hashCode(isEndTurnActionAvailable(currentPlayer));
        signature = signature * 31 + countUnownedProperties();
        signature = signature * 31 + (StreetProperty.BANK_HOUSE_SUPPLY - players.getTotalHouseCount());
        signature = signature * 31 + (StreetProperty.BANK_HOTEL_SUPPLY - players.getTotalHotelCount());
        DebtState debtState = debtController.debtState();
        if (debtState != null) {
            signature = signature * 31 + debtState.paymentRequest().amount();
            signature = signature * 31 + debtState.paymentRequest().reason().hashCode();
            signature = signature * 31 + Boolean.hashCode(debtState.bankruptcyRisk());
        }
        Property offeredProperty = runtime.popupService().activeOfferedProperty();
        if (offeredProperty != null) {
            signature = signature * 31 + offeredProperty.getSpotType().ordinal();
        }
        for (Player player : players.getPlayers()) {
            signature = signature * 31 + buildPlayerViewSignature(player);
        }
        return signature;
    }

    private long buildPlayerViewSignature(Player player) {
        long signature = player.getId();
        signature = signature * 31 + player.getMoneyAmount();
        signature = signature * 31 + player.getTurnNumber();
        signature = signature * 31 + player.getSpot().getSpotType().ordinal();
        signature = signature * 31 + Boolean.hashCode(player.isInJail());
        signature = signature * 31 + JailSpot.jailTimeLeftMap.getOrDefault(player, 0);
        signature = signature * 31 + player.getGetOutOfJailCardCount();
        signature = signature * 31 + player.getTotalHouseCount();
        signature = signature * 31 + player.getTotalHotelCount();
        signature = signature * 31 + player.getTotalLiquidationValue();
        signature = signature * 31 + calculateBoardDangerScore(player);
        for (Property property : player.getOwnedProperties()) {
            signature = signature * 31 + property.getSpotType().ordinal();
            signature = signature * 31 + property.getPrice();
            signature = signature * 31 + Boolean.hashCode(property.isMortgaged());
            signature = signature * 31 + property.getLiquidationValue();
            if (property instanceof StreetProperty streetProperty) {
                signature = signature * 31 + streetProperty.getHousePrice();
                signature = signature * 31 + streetProperty.getBuildingLevel();
                signature = signature * 31 + streetProperty.getHouseCount();
                signature = signature * 31 + streetProperty.getHotelCount();
            }
        }
        return signature;
    }

    private boolean isRollDiceActionAvailable(Player currentPlayer) {
        if (currentPlayer == null || runtime.popupService().isAnyVisible() || debtController.debtState() != null) {
            return false;
        }
        if (!currentPlayer.isComputerControlled()) {
            return dices.isVisible();
        }
        return dices.getValue() == null;
    }

    private boolean isEndTurnActionAvailable(Player currentPlayer) {
        if (currentPlayer == null || runtime.popupService().isAnyVisible() || debtController.debtState() != null) {
            return false;
        }
        if (!currentPlayer.isComputerControlled()) {
            return endRoundButton.isVisible();
        }
        return dices.getValue() != null;
    }

    private PropertyView createPropertyView(Player owner, Property property) {
        int housePrice = 0;
        int buildingLevel = 0;
        int houseCount = 0;
        int hotelCount = 0;
        if (property instanceof StreetProperty streetProperty) {
            housePrice = streetProperty.getHousePrice();
            buildingLevel = streetProperty.getBuildingLevel();
            houseCount = streetProperty.getHouseCount();
            hotelCount = streetProperty.getHotelCount();
        }
        return new PropertyView(
                property.getSpotType(),
                property.getSpotType().streetType,
                property.getSpotType().streetType.placeType,
                property.getDisplayName(),
                property.getPrice(),
                property.isMortgaged(),
                property.getMortgageValue(),
                property.getLiquidationValue(),
                housePrice,
                buildingLevel,
                houseCount,
                hotelCount,
                estimateRent(property, owner),
                owner.ownsAllStreetProperties(property.getSpotType().streetType)
        );
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

    private String debtTargetName(PaymentRequest paymentRequest) {
        if (paymentRequest.target() instanceof PlayerTarget playerTarget) {
            return playerTarget.player().getName();
        }
        return paymentRequest.target().getClass().getSimpleName();
    }

    private PropertyView createPopupPropertyView(Player currentPlayer) {
        Property offeredProperty = runtime.popupService().activeOfferedProperty();
        if (offeredProperty == null) {
            return null;
        }
        return new PropertyView(
                offeredProperty.getSpotType(),
                offeredProperty.getSpotType().streetType,
                offeredProperty.getSpotType().streetType.placeType,
                offeredProperty.getDisplayName(),
                offeredProperty.getPrice(),
                offeredProperty.isMortgaged(),
                offeredProperty.getMortgageValue(),
                offeredProperty.getLiquidationValue(),
                0,
                0,
                0,
                0,
                estimateOfferedPropertyRent(offeredProperty, currentPlayer),
                currentPlayer != null && currentPlayer.countOwnedProperties(offeredProperty.getSpotType().streetType) + 1
                        >= SpotType.getNumberOfSpots(offeredProperty.getSpotType().streetType)
        );
    }

    private int estimateOfferedPropertyRent(Property property, Player currentPlayer) {
        if (property.getSpotType().streetType.placeType == PlaceType.UTILITY) {
            int utilityCount = currentPlayer == null ? 0 : currentPlayer.countOwnedProperties(property.getSpotType().streetType) + 1;
            return utilityCount >= 2 ? 70 : 28;
        }
        Player simulatedVisitor = null;
        for (Player candidate : players.getPlayers()) {
            if (candidate != currentPlayer) {
                simulatedVisitor = candidate;
                break;
            }
        }
        if (simulatedVisitor == null) {
            return 0;
        }
        Player originalOwner = property.getOwnerPlayer();
        property.setOwnerPlayer(currentPlayer);
        try {
            return property.getRent(simulatedVisitor);
        } finally {
            property.setOwnerPlayer(originalOwner);
        }
    }
}
