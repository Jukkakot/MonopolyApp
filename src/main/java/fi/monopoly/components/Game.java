package fi.monopoly.components;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.computer.*;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.components.payment.*;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.trade.TradeDecision;
import fi.monopoly.components.trade.TradeOffer;
import fi.monopoly.components.trade.TradeOfferEvaluator;
import fi.monopoly.components.trade.TradeSelection;
import fi.monopoly.components.turn.*;
import fi.monopoly.types.*;
import fi.monopoly.utils.TextWrapUtils;
import fi.monopoly.utils.LayoutMetrics;
import javafx.scene.paint.Color;
import lombok.extern.slf4j.Slf4j;
import processing.core.PConstants;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static fi.monopoly.text.UiTexts.text;
import static processing.event.MouseEvent.CLICK;

@Slf4j
public class Game implements MonopolyEventListener {
    private static final boolean FORCE_DEBT_DEBUG_SCENARIO = false;
    private static final LayoutMetrics DEFAULT_LAYOUT = LayoutMetrics.defaultWindow();
    private static final float SIDEBAR_X = DEFAULT_LAYOUT.sidebarX();
    private static final float SIDEBAR_W = DEFAULT_LAYOUT.sidebarWidth();
    private static final int SIDEBAR_MARGIN = 16;
    private static final int SIDEBAR_LABEL_X = 16;
    private static final int SIDEBAR_VALUE_X = 192;
    private static final int SIDEBAR_MIN_CONTENT_TOP = 220;
    private static final int SIDEBAR_HISTORY_HEIGHT = 192;
    private static final int SIDEBAR_HISTORY_MIN_HEIGHT = 112;
    private static final int SIDEBAR_HISTORY_HEADER_HEIGHT = 32;
    private static final int SIDEBAR_HISTORY_TEXT_INSET = 8;
    private static final int SIDEBAR_HISTORY_ENTRY_HEIGHT = 28;
    private static final int SIDEBAR_HISTORY_TOP_MARGIN = 16;
    private static final int SIDEBAR_LINE_HEIGHT = 24;
    private static final int OVERLAY_MARGIN = 16;
    private static final int OVERLAY_PRIMARY_BUTTON_Y = 16;
    private static final int OVERLAY_SECONDARY_ROW_1_Y = 68;
    private static final int OVERLAY_SECONDARY_ROW_2_Y = 112;
    private static final int OVERLAY_SECONDARY_ROW_3_Y = 156;
    private static final int COMPUTER_ACTION_DELAY_MS = 120;
    private static final int NO_COMPUTER_ACTION_YET = -1;
    private static Game current;
    public static Dices DICES;
    public static Players players;
    public static Animations animations;
    public static int GO_MONEY_AMOUNT = 200;
    private final MonopolyRuntime runtime;
    private final TurnEngine turnEngine = new TurnEngine();
    private final PaymentResolver paymentResolver = new PaymentResolver();
    private final MonopolyButton endRoundButton;
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final MonopolyButton debugAddCashButton;
    private final MonopolyButton debugDebtScenarioButton;
    private final MonopolyButton debugSendToJailButton;
    private final MonopolyButton debugResetTurnButton;
    private final MonopolyButton debugGodModeButton;
    private final MonopolyButton pauseButton;
    private final MonopolyButton tradeButton;
    private final MonopolyButton languageButton;
    Board board;
    TurnResult prevTurnResult;
    private DebtState debtState;
    private int lastComputerActionAt = NO_COMPUTER_ACTION_YET;
    private boolean paused;
    private final TradeOfferEvaluator tradeOfferEvaluator = new TradeOfferEvaluator();

    public Game(MonopolyRuntime runtime) {
        current = this;
        this.runtime = runtime;
        this.endRoundButton = new MonopolyButton(runtime, "endRound");
        endRoundButton.setPosition(SIDEBAR_X + SIDEBAR_VALUE_X, DEFAULT_LAYOUT.sidebarPrimaryButtonY());
        endRoundButton.setSize(150, 44);
        endRoundButton.setAutoWidth(100, 28, 180);
        this.retryDebtButton = new MonopolyButton(runtime, "retryDebt");
        retryDebtButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, DEFAULT_LAYOUT.sidebarPrimaryButtonY());
        retryDebtButton.setSize(140, 40);
        retryDebtButton.setAutoWidth(140, 28, 220);
        this.declareBankruptcyButton = new MonopolyButton(runtime, "declareBankruptcy");
        declareBankruptcyButton.setPosition(SIDEBAR_X + SIDEBAR_VALUE_X, DEFAULT_LAYOUT.sidebarPrimaryButtonY());
        declareBankruptcyButton.setSize(140, 40);
        declareBankruptcyButton.setAutoWidth(140, 28, 220);
        this.debugAddCashButton = new MonopolyButton(runtime, "debugAddCash");
        debugAddCashButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, DEFAULT_LAYOUT.sidebarDebugButtonRow1Y());
        debugAddCashButton.setSize(140, 36);
        debugAddCashButton.setAutoWidth(140, 28, 220);
        this.debugDebtScenarioButton = new MonopolyButton(runtime, "debugDebtScenario");
        debugDebtScenarioButton.setPosition(SIDEBAR_X + SIDEBAR_VALUE_X, DEFAULT_LAYOUT.sidebarDebugButtonRow1Y());
        debugDebtScenarioButton.setSize(140, 36);
        debugDebtScenarioButton.setAutoWidth(140, 28, 220);
        this.debugSendToJailButton = new MonopolyButton(runtime, "debugSendToJail");
        debugSendToJailButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, DEFAULT_LAYOUT.sidebarDebugButtonRow2Y());
        debugSendToJailButton.setSize(140, 36);
        debugSendToJailButton.setAutoWidth(140, 28, 220);
        this.debugResetTurnButton = new MonopolyButton(runtime, "debugResetTurn");
        debugResetTurnButton.setPosition(SIDEBAR_X + SIDEBAR_VALUE_X, DEFAULT_LAYOUT.sidebarDebugButtonRow2Y());
        debugResetTurnButton.setSize(140, 36);
        debugResetTurnButton.setAutoWidth(140, 28, 220);
        this.debugGodModeButton = new MonopolyButton(runtime, "debugGodMode");
        debugGodModeButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, DEFAULT_LAYOUT.sidebarDebugButtonRow3Y());
        debugGodModeButton.setSize(300, 36);
        debugGodModeButton.setAutoWidth(180, 28, 300);
        this.pauseButton = new MonopolyButton(runtime, "pause");
        pauseButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, runtime.app().height - 96);
        pauseButton.setSize(140, 36);
        pauseButton.setAutoWidth(120, 28, 180);
        this.tradeButton = new MonopolyButton(runtime, "trade");
        tradeButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, runtime.app().height - 96);
        tradeButton.setSize(140, 36);
        tradeButton.setAutoWidth(120, 28, 220);
        this.languageButton = new MonopolyButton(runtime, "language");
        languageButton.setPosition(SIDEBAR_X + SIDEBAR_MARGIN, runtime.app().height - 48);
        languageButton.setSize(220, 36);
        languageButton.setAutoWidth(180, 28, 280);
        refreshLabels();
        endRoundButton.hide();
        retryDebtButton.hide();
        declareBankruptcyButton.hide();
        debugAddCashButton.hide();
        debugDebtScenarioButton.hide();
        debugSendToJailButton.hide();
        debugResetTurnButton.hide();
        debugGodModeButton.hide();
        pauseButton.hide();
        tradeButton.hide();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
        runtime.eventBus().addListener(this);
        PropertyFactory.resetState();
        JailSpot.jailTimeLeftMap.clear();
        board = new Board(runtime);
        DICES = Dices.setRollDice(runtime, this::rollDice);
        players = new Players(runtime);
        animations = new Animations();

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

        endRoundButton.addListener(e -> {
            if (!runtime.popupService().isAnyVisible() && debtState == null) {
                endRound(true);
            }
        });
        retryDebtButton.addListener(this::retryPendingDebtPayment);
        declareBankruptcyButton.addListener(this::declareBankruptcy);
        debugAddCashButton.addListener(this::debugAddCash);
        debugDebtScenarioButton.addListener(() -> debugStartDebtScenario(200));
        debugSendToJailButton.addListener(this::debugSendCurrentPlayerToJail);
        debugResetTurnButton.addListener(this::debugResetTurnState);
        debugGodModeButton.addListener(this::openDebugGodModeMenu);
        pauseButton.addListener(this::togglePause);
        tradeButton.addListener(this::openTradeMenu);
        languageButton.addListener(this::openLanguageMenu);

        if (FORCE_DEBT_DEBUG_SCENARIO) {
            initializeDebtDebugScenario();
        }
    }

    public static boolean isDebtResolutionActive() {
        return current != null && current.debtState != null;
    }

    public static boolean isDebtResolutionForCurrentTurn() {
        return isDebtResolutionActive();
    }

    public void draw() {
        LayoutMetrics layoutMetrics = getLayoutMetrics();
        boolean hasSidebarSpace = layoutMetrics.hasSidebarSpace();
        if (MonopolyApp.SKIP_ANNIMATIONS) {
            animations.finishAllAnimations();
        }
        if (!runtime.popupService().isAnyVisible()) {
            animations.updateAnimations();
        }
        updateSidebarControlPositions();
        board.draw(null);
        if (hasSidebarSpace) {
            drawSidebarPanel();
        }
        if (!isDebtSidebarMode()) {
            DICES.draw(null);
        }
        if (hasSidebarSpace && isDebtSidebarMode()) {
            players.focusPlayer(debtState.paymentRequest().debtor());
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
    }

    /**
     * Draws the persistent right-side information panel so turn state, player
     * overview and controls stay in one predictable area.
     */
    private void drawSidebarPanel() {
        LayoutMetrics layoutMetrics = getLayoutMetrics();
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
        app.line(sidebarX + SIDEBAR_MARGIN, layoutMetrics.sidebarHeaderHeight(), sidebarX + sidebarWidth - SIDEBAR_MARGIN, layoutMetrics.sidebarHeaderHeight());
        if (isDebtSidebarMode()) {
            app.line(sidebarX + SIDEBAR_MARGIN, getDebtSectionBottom(), sidebarX + sidebarWidth - SIDEBAR_MARGIN, getDebtSectionBottom());
        }

        Player turnPlayer = players.getTurn();
        app.fill(46, 72, 63);
        app.textAlign(PConstants.LEFT);
        app.textFont(runtime.font30());
        app.text(text("sidebar.title"), sidebarX + SIDEBAR_MARGIN, layoutMetrics.sidebarTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.text(text("sidebar.currentPlayer"), sidebarX + SIDEBAR_LABEL_X, layoutMetrics.sidebarHeaderRow1Y());
        app.text(text("sidebar.turnPhase"), sidebarX + SIDEBAR_LABEL_X, layoutMetrics.sidebarHeaderRow2Y());
        app.text(text("sidebar.currentSpot"), sidebarX + SIDEBAR_LABEL_X, layoutMetrics.sidebarHeaderRow3Y());

        app.fill(46, 72, 63);
        float currentPlayerValueX = sidebarX + SIDEBAR_VALUE_X;
        float currentPlayerValueY = layoutMetrics.sidebarHeaderRow1Y();
        app.text(turnPlayer != null ? turnPlayer.getName() : text("sidebar.none"), currentPlayerValueX, currentPlayerValueY);
        if (turnPlayer != null && turnPlayer.isComputerControlled()) {
            drawComputerBadge(app, currentPlayerValueX + app.textWidth(turnPlayer.getName()) + 12, currentPlayerValueY - 14);
        }
        app.text(resolveCurrentTurnPhase(), sidebarX + SIDEBAR_VALUE_X, layoutMetrics.sidebarHeaderRow2Y());
        app.text(turnPlayer != null && turnPlayer.getSpot() != null ? turnPlayer.getSpot().getName() : text("sidebar.none"), sidebarX + SIDEBAR_VALUE_X, layoutMetrics.sidebarHeaderRow3Y());
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
        float panelX = layoutMetrics.sidebarX() + SIDEBAR_MARGIN;
        float historyHeight = getSidebarHistoryHeight();
        float panelY = getSidebarHistoryPanelY();
        float panelW = layoutMetrics.sidebarWidth() - SIDEBAR_MARGIN * 2;
        List<String> recentMessages = runtime.popupService().recentPopupMessages();

        app.noStroke();
        app.fill(255, 249, 233);
        app.rect(panelX, panelY, panelW, historyHeight, 16);

        app.stroke(193, 178, 140);
        app.strokeWeight(2);
        app.line(panelX, panelY + SIDEBAR_HISTORY_HEADER_HEIGHT, panelX + panelW, panelY + SIDEBAR_HISTORY_HEADER_HEIGHT);

        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.history"), panelX + SIDEBAR_HISTORY_TEXT_INSET, panelY + 24);

        app.fill(0);
        app.textFont(runtime.font10());
        app.textAlign(PConstants.LEFT, PConstants.TOP);

        if (recentMessages.isEmpty()) {
            app.text(
                    text("sidebar.history.empty"),
                    panelX + SIDEBAR_HISTORY_TEXT_INSET,
                    panelY + 56,
                    panelW - SIDEBAR_HISTORY_TEXT_INSET * 2,
                    historyHeight - 48
            );
            return;
        }

        float currentY = panelY + 52;
        float maxTextWidth = panelW - SIDEBAR_HISTORY_TEXT_INSET * 2;
        for (String message : recentMessages) {
            String condensedMessage = message.replaceAll("\\R+", " / ").replaceAll("\\s{2,}", " ").trim();
            if (condensedMessage.isEmpty()) {
                continue;
            }
            List<String> wrappedLines = TextWrapUtils.wrapText(app.g, "- " + condensedMessage, maxTextWidth);
            for (String line : wrappedLines) {
                app.text(line, panelX + SIDEBAR_HISTORY_TEXT_INSET, currentY);
                currentY += 14;
                if (currentY > panelY + historyHeight - 16) {
                    return;
                }
            }
            currentY += 6;
            if (currentY > panelY + historyHeight - 16) {
                break;
            }
        }
    }

    private String resolveCurrentTurnPhase() {
        if (debtState != null) {
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
        if (DICES.isVisible()) {
            return text("sidebar.phase.roll");
        }
        return text("sidebar.phase.resolving");
    }

    private float getSidebarContentTop() {
        if (debtState != null) {
            return getDebtSectionBottom() + 20;
        }
        float debugFloor = getSidebarReservedTop();
        float historyTop = getSidebarHistoryPanelY();
        float availableTop = historyTop - SIDEBAR_HISTORY_TOP_MARGIN;
        return Math.max(SIDEBAR_MIN_CONTENT_TOP, Math.min(debugFloor, availableTop));
    }

    private boolean isDebtSidebarMode() {
        return debtState != null;
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
        if (debtState == null) {
            return Math.round(getLayoutMetrics().debtTextY());
        }
        int lineCount = buildDebtSidebarText(debtState.paymentRequest()).split("\\R").length;
        return Math.round(getLayoutMetrics().debtTextY() + lineCount * SIDEBAR_LINE_HEIGHT);
    }

    private Spot getNewSpot(DiceValue diceValue) {
        Player turn = players.getTurn();
        Spot oldSpot = turn.getSpot();
        return board.getNewSpot(oldSpot, diceValue.value(), PathMode.NORMAL);
    }

    private void rollDice() {
        if (runtime.popupService().isAnyVisible() || debtState != null) {
            log.trace("Ignoring rollDice because popupVisible={} debtStateActive={}",
                    runtime.popupService().isAnyVisible(), debtState != null);
            return;
        }
        log.debug("Starting turn roll for player {}", players.getTurn().getName());
        playRound(DICES.getValue());
    }

    private void runComputerPlayerStep() {
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
        boolean shouldApplyDelay = !MonopolyApp.SKIP_ANNIMATIONS && lastComputerActionAt != NO_COMPUTER_ACTION_YET;
        if (shouldApplyDelay && now - lastComputerActionAt < COMPUTER_ACTION_DELAY_MS) {
            return;
        }

        boolean acted = ComputerStrategies.forProfile(turnPlayer.getComputerProfile())
                .takeStep(new GameComputerTurnContext(turnPlayer));
        if (acted) {
            lastComputerActionAt = now;
        }
    }

    private void endRound(boolean switchTurns) {
        Player currentTurn = players.getTurn();
        prevTurnResult = null;
        if (switchTurns) {
            players.switchTurn();
        }
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
                runtime.popupService().show(text("game.go.reward", GO_MONEY_AMOUNT), () -> {
                    turnPlayer.addMoney(GO_MONEY_AMOUNT);
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
        GameState gameState = new GameState(players, DICES, board, TurnResult.copyOf(prevTurnResult), this::handlePaymentRequest);
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
        boolean consumedEvent = false;
        if (event instanceof KeyEvent keyEvent) {
            char key = Character.toLowerCase(keyEvent.getKey());
            if (key == 'p') {
                togglePause();
                return true;
            }
            if (runtime.popupService().isAnyVisible()) {
                return consumedEvent;
            }
            if (debtState != null) {
                if (key == MonopolyApp.SPACE || key == MonopolyApp.ENTER) {
                    retryPendingDebtPayment();
                    return true;
                }
                return false;
            }
            if (key == 't') {
                openTradeMenu();
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
                openDebugGodModeMenu();
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
                if (hoveredSpot != null && MonopolyApp.DEBUG_MODE && DICES.isVisible()) {
                    DICES.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
                    if (playRound(hoveredSpot, DiceState.DEBUG_REROLL)) {
                        consumedEvent = true;
                        DICES.hide();
                    }
                }
            }
        }
        return consumedEvent;
    }

    private void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        log.debug("Handling payment request in Game: debtor={}, target={}, amount={}, reason={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), request.reason());
        PaymentResult result = paymentResolver.tryPay(request);
        if (result.status() == PaymentStatus.PAID) {
            log.trace("Payment completed immediately in Game");
            onResolved.doAction();
            return;
        }

        debtState = new DebtState(request, onResolved, result.status() == PaymentStatus.BANKRUPT);
        hidePrimaryTurnControls();
        updateDebtButtons();
        log.info("Entering debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    private void retryPendingDebtPayment() {
        if (debtState == null) {
            return;
        }

        log.debug("Retrying pending debt payment for debtor={} amount={}",
                debtState.paymentRequest().debtor().getName(), debtState.paymentRequest().amount());
        PaymentResult result = paymentResolver.tryPay(debtState.paymentRequest());
        if (result.status() == PaymentStatus.PAID) {
            DebtState resolvedDebt = debtState;
            debtState = null;
            updateDebtButtons();
            log.info("Debt resolved for {}", resolvedDebt.paymentRequest().debtor().getName());
            runtime.popupService().show(text("game.debt.paid", resolvedDebt.paymentRequest().reason()), resolvedDebt.onResolved()::doAction);
            return;
        }

        debtState = new DebtState(debtState.paymentRequest(), debtState.onResolved(), result.status() == PaymentStatus.BANKRUPT);
        updateDebtButtons();
        log.info("Debt still unresolved for {}. bankruptcyRisk={}",
                debtState.paymentRequest().debtor().getName(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    // Auto-mortgage removed: player must choose which properties to mortgage manually

    private String buildDebtMessage(PaymentResult result) {
        if (debtState == null) {
            return text("game.debt.couldNotComplete");
        }

        PaymentRequest request = debtState.paymentRequest();
        StringBuilder text = new StringBuilder(text(
                "game.debt.message",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.reason(),
                request.debtor().getMoneyAmount(),
                result.missingAmount()
        ));
        if (result.status() == PaymentStatus.BANKRUPT) {
            text.append(text("game.debt.message.bankruptcyLine"));
        }
        return text.toString();
    }

    private void drawDebtState() {
        if (debtState == null) {
            return;
        }

        LayoutMetrics layoutMetrics = getLayoutMetrics();
        MonopolyApp app = runtime.app();
        app.push();
        app.fill(46, 72, 63);
        app.textFont(runtime.font20());
        app.text(text("sidebar.section.debt"), layoutMetrics.sidebarX() + SIDEBAR_MARGIN, layoutMetrics.debtSectionTitleY());

        app.fill(0);
        app.textFont(runtime.font20());
        app.textAlign(PConstants.LEFT);
        PaymentRequest request = debtState.paymentRequest();
        app.text(
                buildDebtSidebarText(request),
                layoutMetrics.sidebarX() + SIDEBAR_MARGIN,
                layoutMetrics.debtTextY()
        );
        app.pop();
    }

    private LayoutMetrics getLayoutMetrics() {
        return LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
    }

    private void updateSidebarControlPositions() {
        LayoutMetrics layoutMetrics = getLayoutMetrics();
        if (!layoutMetrics.hasSidebarSpace()) {
            layoutOverlayControls(layoutMetrics);
            DICES.updateLayout(layoutMetrics);
            return;
        }

        float sidebarLeftX = layoutMetrics.sidebarX() + SIDEBAR_MARGIN;
        float sidebarRightAlignedX = layoutMetrics.sidebarRight() - SIDEBAR_MARGIN;
        float primaryButtonY = layoutMetrics.sidebarPrimaryButtonY();
        float debugRow1Y = layoutMetrics.sidebarDebugButtonRow1Y();
        float debugRow2Y = layoutMetrics.sidebarDebugButtonRow2Y();
        float debugRow3Y = layoutMetrics.sidebarDebugButtonRow3Y();
        float pauseButtonY = getSidebarHistoryPanelY() + getSidebarHistoryHeight() + 12;
        float tradeButtonY = pauseButtonY;
        float languageButtonY = getSidebarHistoryPanelY() + getSidebarHistoryHeight() + 12;

        endRoundButton.setPosition(sidebarLeftX, primaryButtonY);
        retryDebtButton.setPosition(sidebarLeftX, primaryButtonY);
        declareBankruptcyButton.setPosition(sidebarRightAlignedX - declareBankruptcyButton.getWidth(), primaryButtonY);
        debugAddCashButton.setPosition(sidebarLeftX, debugRow1Y);
        debugDebtScenarioButton.setPosition(sidebarRightAlignedX - debugDebtScenarioButton.getWidth(), debugRow1Y);
        debugSendToJailButton.setPosition(sidebarLeftX, debugRow2Y);
        debugResetTurnButton.setPosition(sidebarRightAlignedX - debugResetTurnButton.getWidth(), debugRow2Y);
        debugGodModeButton.setPosition(sidebarLeftX, debugRow3Y);
        languageButton.setPosition(sidebarRightAlignedX - languageButton.getWidth(), languageButtonY);
        pauseButton.setPosition(languageButton.getPosition()[0] - pauseButton.getWidth() - 8, pauseButtonY);
        tradeButton.setPosition(pauseButton.getPosition()[0] - tradeButton.getWidth() - 8, tradeButtonY);
        DICES.updateLayout(layoutMetrics);
    }

    private void layoutOverlayControls(LayoutMetrics layoutMetrics) {
        float leftX = OVERLAY_MARGIN;
        float rightX = layoutMetrics.boardWidth() - OVERLAY_MARGIN;

        endRoundButton.setPosition(leftX, OVERLAY_PRIMARY_BUTTON_Y);
        retryDebtButton.setPosition(leftX, OVERLAY_PRIMARY_BUTTON_Y);
        declareBankruptcyButton.setPosition(rightX - declareBankruptcyButton.getWidth(), OVERLAY_PRIMARY_BUTTON_Y);
        debugAddCashButton.setPosition(leftX, OVERLAY_SECONDARY_ROW_1_Y);
        debugDebtScenarioButton.setPosition(rightX - debugDebtScenarioButton.getWidth(), OVERLAY_SECONDARY_ROW_1_Y);
        debugSendToJailButton.setPosition(leftX, OVERLAY_SECONDARY_ROW_2_Y);
        debugResetTurnButton.setPosition(rightX - debugResetTurnButton.getWidth(), OVERLAY_SECONDARY_ROW_2_Y);
        debugGodModeButton.setPosition(leftX, OVERLAY_SECONDARY_ROW_3_Y);
        languageButton.setPosition(
                Math.max(leftX, rightX - languageButton.getWidth()),
                OVERLAY_SECONDARY_ROW_3_Y
        );
        pauseButton.setPosition(
                Math.max(leftX, languageButton.getPosition()[0] - pauseButton.getWidth() - 8),
                OVERLAY_SECONDARY_ROW_3_Y
        );
        tradeButton.setPosition(
                Math.max(leftX, pauseButton.getPosition()[0] - tradeButton.getWidth() - 8),
                OVERLAY_SECONDARY_ROW_3_Y
        );
    }

    private float getSidebarHistoryHeight() {
        float availableHeight = runtime.app().height - getSidebarReservedTop() - getLayoutMetrics().sidebarHistoryBottomMargin() - SIDEBAR_HISTORY_TOP_MARGIN;
        return Math.max(SIDEBAR_HISTORY_MIN_HEIGHT, Math.min(SIDEBAR_HISTORY_HEIGHT, availableHeight));
    }

    private float getSidebarHistoryPanelY() {
        return runtime.app().height - getSidebarHistoryHeight() - getLayoutMetrics().sidebarHistoryBottomMargin();
    }

    private float getSidebarReservedTop() {
        return getLayoutMetrics().sidebarReservedTop(MonopolyApp.DEBUG_MODE);
    }

    private void declareBankruptcy() {
        if (debtState == null) {
            return;
        }
        if (!debtState.bankruptcyRisk()) {
            runtime.popupService().show(text("game.debt.assetsCover"));
            return;
        }
        PaymentRequest request = debtState.paymentRequest();
        log.warn("Declaring bankruptcy: debtor={}, target={}, amount={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount());
        int liquidationCash = request.debtor().liquidateBuildingsToBank();
        if (liquidationCash > 0) {
            log.info("Bankruptcy liquidation sold buildings for debtor={} and raised M{}",
                    request.debtor().getName(), liquidationCash);
        }
        if (request.target() instanceof PlayerTarget playerTarget) {
            request.debtor().transferAssetsTo(playerTarget.player());
            finishBankruptcyFlow(request);
        } else {
            List<Property> bankAuctionProperties = List.copyOf(request.debtor().getOwnedProperties());
            request.debtor().releaseAssetsToBank();
            finishBankruptcyFlowAfterBankRelease(request, bankAuctionProperties);
            return;
        }
    }

    private void finishBankruptcyFlowAfterBankRelease(PaymentRequest request, List<Property> bankAuctionProperties) {
        log.info("Bankruptcy returned {} debtor properties to bank for auction", bankAuctionProperties.size());
        removeBankruptPlayerFromGame(request);
        new PropertyAuctionResolver(runtime.popupService(), players).resolveAll(bankAuctionProperties, () -> completeBankruptcyFlow(request));
    }

    private void finishBankruptcyFlow(PaymentRequest request) {
        removeBankruptPlayerFromGame(request);
        completeBankruptcyFlow(request);
    }

    private void removeBankruptPlayerFromGame(PaymentRequest request) {
        request.debtor().setGetOutOfJailCardCount(0);
        players.removePlayer(request.debtor());
        debtState = null;
        updateDebtButtons();
    }

    private void completeBankruptcyFlow(PaymentRequest request) {
        if (players.count() <= 1) {
            Player winner = players.getPlayers().stream().findFirst().orElse(null);
            if (winner != null && winner.getSpot() != null) {
                winner.setCoords(winner.getSpot().getTokenCoords(winner));
                players.focusPlayer(winner);
            }
            String winnerName = winner != null ? winner.getName() : text("game.bankruptcy.noWinner");
            log.info("Game over after bankruptcy. winner={}", winnerName);
            runtime.popupService().show(text("game.bankruptcy.gameOver", winnerName));
            hidePrimaryTurnControls();
            return;
        }

        players.switchTurn();
        showRollDiceControl();
        log.info("Bankruptcy handled. Next turn player={}", players.getTurn() != null ? players.getTurn().getName() : "none");
        runtime.popupService().show(text("game.bankruptcy.playerWent", request.debtor().getName()));
    }

    private void updateDebtButtons() {
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

    private void initializeDebtDebugScenario() {
        Player turnPlayer = players.getTurn();
        turnPlayer.buyProperty(PropertyFactory.getProperty(SpotType.RR1));
        turnPlayer.addMoney(-(turnPlayer.getMoneyAmount() - 40));
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, text("game.debug.reason.tax", 200)),
                this::restoreNormalTurnControls
        );
    }

    private void updateDebugButtons() {
        pauseButton.show();
        tradeButton.show();
        languageButton.show();
        if (!MonopolyApp.DEBUG_MODE) {
            debugAddCashButton.hide();
            debugDebtScenarioButton.hide();
            debugSendToJailButton.hide();
            debugResetTurnButton.hide();
            debugGodModeButton.hide();
            return;
        }
        debugAddCashButton.show();
        debugDebtScenarioButton.show();
        debugSendToJailButton.show();
        debugResetTurnButton.show();
        debugGodModeButton.show();
    }

    private void refreshLabels() {
        endRoundButton.setLabel(text("game.button.endRound"));
        retryDebtButton.setLabel(text("game.button.retryDebt"));
        declareBankruptcyButton.setLabel(text("game.button.bankrupt"));
        debugAddCashButton.setLabel(text("game.button.debugAddCash"));
        debugDebtScenarioButton.setLabel(text("game.button.debugDebt"));
        debugSendToJailButton.setLabel(text("game.button.debugJail"));
        debugResetTurnButton.setLabel(text("game.button.debugReset"));
        debugGodModeButton.setLabel(text("game.button.godMode"));
        pauseButton.setLabel(paused ? text("game.button.resume") : text("game.button.pause"));
        tradeButton.setLabel(text("game.button.trade"));
        languageButton.setLabel(text("language.button.current", text("language.name.current")));
    }

    private void togglePause() {
        paused = !paused;
        refreshLabels();
        log.info("Game paused={}", paused);
    }

    private void openLanguageMenu() {
        runtime.popupService().show(
                text("language.menu.title"),
                new ButtonProps(text("language.menu.english"), () -> switchLanguage(Locale.ENGLISH)),
                new ButtonProps(text("language.menu.finnish"), () -> switchLanguage(Locale.forLanguageTag("fi")))
        );
    }

    private void openTradeMenu() {
        if (runtime.popupService().isAnyVisible() || debtState != null) {
            return;
        }
        Player proposer = players.getTurn();
        if (proposer == null) {
            return;
        }
        List<Player> tradePartners = players.getPlayers().stream()
                .filter(player -> player != proposer)
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .toList();
        if (tradePartners.isEmpty()) {
            runtime.popupService().show(text("trade.noPartners"));
            return;
        }
        ButtonProps[] partnerButtons = tradePartners.stream()
                .map(player -> new ButtonProps(player.getName(), () -> openTradeOfferSelection(proposer, player)))
                .toArray(ButtonProps[]::new);
        runtime.popupService().show(text("trade.choosePartner", proposer.getName()), partnerButtons);
    }

    private void openTradeOfferSelection(Player proposer, Player recipient) {
        runtime.popupService().show(
                text("trade.chooseOffer", proposer.getName(), recipient.getName()),
                buildTradeSelectionButtons(proposer, selection -> openTradeRequestSelection(proposer, recipient, selection))
        );
    }

    private void openTradeRequestSelection(Player proposer, Player recipient, TradeSelection offeredSelection) {
        runtime.popupService().show(
                text("trade.chooseRequest", recipient.getName(), proposer.getName()),
                buildTradeSelectionButtons(recipient, selection -> confirmTradeOffer(new TradeOffer(proposer, recipient, offeredSelection, selection)))
        );
    }

    private ButtonProps[] buildTradeSelectionButtons(Player owner, java.util.function.Consumer<TradeSelection> onSelect) {
        List<ButtonProps> buttons = new java.util.ArrayList<>();
        buttons.add(new ButtonProps(text("trade.option.nothing"), () -> onSelect.accept(TradeSelection.NONE)));
        for (int amount : List.of(50, 100, 200, 500)) {
            if (owner.getMoneyAmount() >= amount) {
                buttons.add(new ButtonProps(text("trade.option.money", amount), () -> onSelect.accept(new TradeSelection(amount, null, false))));
            }
        }
        owner.getOwnedProperties().stream()
                .sorted(Comparator.comparingInt(property -> property.getSpotType().ordinal()))
                .filter(this::isTradableProperty)
                .forEach(property -> buttons.add(new ButtonProps(
                        text("trade.option.property", property.getDisplayName()),
                        () -> onSelect.accept(new TradeSelection(0, property, false))
                )));
        if (owner.hasGetOutOfJailCard()) {
            buttons.add(new ButtonProps(text("trade.option.jailCard"), () -> onSelect.accept(new TradeSelection(0, null, true))));
        }
        return buttons.toArray(ButtonProps[]::new);
    }

    private boolean isTradableProperty(Property property) {
        return !(property instanceof StreetProperty streetProperty) || streetProperty.getBuildingLevel() == 0;
    }

    private void confirmTradeOffer(TradeOffer offer) {
        if (!offer.isValid()) {
            runtime.popupService().show(text("trade.invalid"));
            return;
        }
        String summary = buildTradeSummary(offer);
        if (offer.recipient().isComputerControlled()) {
            TradeDecision decision = tradeOfferEvaluator.evaluateForRecipient(offer);
            log.info("Bot trade decision: player={}, accept={}, score={}, reason={}",
                    offer.recipient().getName(),
                    decision.accept(),
                    Math.round(decision.score() * 10.0) / 10.0,
                    decision.reason());
            if (decision.accept()) {
                applyTradeOffer(offer);
                runtime.popupService().show(text("trade.accepted", offer.recipient().getName()) + "\n" + summary);
            } else {
                runtime.popupService().show(text("trade.declined", offer.recipient().getName()) + "\n" + decision.reason());
            }
            return;
        }
        runtime.popupService().show(
                text("trade.review", offer.recipient().getName()) + "\n" + summary,
                () -> {
                    applyTradeOffer(offer);
                    runtime.popupService().show(text("trade.accepted", offer.recipient().getName()));
                },
                () -> runtime.popupService().show(text("trade.declined", offer.recipient().getName()))
        );
    }

    private void applyTradeOffer(TradeOffer offer) {
        if (!offer.apply()) {
            runtime.popupService().show(text("trade.invalid"));
        }
    }

    private String buildTradeSummary(TradeOffer offer) {
        return text("trade.summary.header", offer.proposer().getName(), offer.recipient().getName())
                + "\n" + text("trade.summary.offer", describeTradeSelection(offer.offeredToRecipient()))
                + "\n" + text("trade.summary.request", describeTradeSelection(offer.requestedFromRecipient()));
    }

    private String describeTradeSelection(TradeSelection selection) {
        if (selection.isEmpty()) {
            return text("trade.option.nothing");
        }
        List<String> parts = new java.util.ArrayList<>();
        if (selection.moneyAmount() > 0) {
            parts.add(text("trade.option.money", selection.moneyAmount()));
        }
        if (selection.property() != null) {
            parts.add(text("trade.option.property", selection.property().getDisplayName()));
        }
        if (selection.jailCard()) {
            parts.add(text("trade.option.jailCard"));
        }
        return String.join(", ", parts);
    }

    private void switchLanguage(Locale locale) {
        fi.monopoly.text.UiTexts.setLocale(locale);
    }

    private void debugAddCash() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer != null) {
            log.debug("Debug action: add cash to {}", turnPlayer.getName());
            turnPlayer.addMoney(500);
            runtime.popupService().show(text("game.debug.receivedCash", turnPlayer.getName(), 500));
        }
    }

    private void debugStartDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: start debt scenario for {}", turnPlayer.getName());
        turnPlayer.addMoney(-(turnPlayer.getMoneyAmount() - 40));
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, text("game.debug.reason.tax", 200)),
                this::restoreNormalTurnControls
        );
    }

    private void debugSendCurrentPlayerToJail() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: send player {} to jail", turnPlayer.getName());
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, JailSpot.JAIL_ROUND_NUMBER);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.sentToJail", turnPlayer.getName()));
    }

    private void debugResetTurnState() {
        log.debug("Debug action: reset turn state");
        animations.finishAllAnimations();
        debtState = null;
        updateDebtButtons();
        runtime.popupService().hideAll();
        showRollDiceControl();
        runtime.popupService().show(text("game.debug.reset"));
    }

    private void openDebugGodModeMenu() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        runtime.popupService().show(
                text("game.debug.godMode.title", turnPlayer.getName()),
                new ButtonProps(text("game.debug.button.money"), this::openDebugMoneyMenu),
                new ButtonProps(text("game.debug.button.move"), this::openDebugMoveMenu),
                new ButtonProps(text("game.debug.button.debt"), this::openDebugDebtMenu),
                new ButtonProps(text("game.debug.button.jail"), this::openDebugJailMenu),
                new ButtonProps(text("game.debug.button.scenarios"), this::openDebugScenarioMenu)
        );
    }

    private void openDebugMoneyMenu() {
        runtime.popupService().show(
                text("game.debug.money.title"),
                new ButtonProps(text("game.debug.money.add500"), () -> debugAdjustCurrentPlayerMoney(500)),
                new ButtonProps(text("game.debug.money.minus100"), () -> debugAdjustCurrentPlayerMoney(-100)),
                new ButtonProps(text("game.debug.money.set0"), () -> debugSetCurrentPlayerMoney(0)),
                new ButtonProps(text("game.debug.money.set50"), () -> debugSetCurrentPlayerMoney(50)),
                new ButtonProps(text("game.debug.money.set1500"), () -> debugSetCurrentPlayerMoney(1500))
        );
    }

    private void openDebugMoveMenu() {
        ButtonProps[] spotButtons = SpotType.SPOT_TYPES.stream()
                .map(spotType -> new ButtonProps(spotType.name(), () -> debugMoveCurrentPlayerTo(spotType)))
                .toArray(ButtonProps[]::new);
        runtime.popupService().show(text("game.debug.move.title"), spotButtons);
    }

    private void openDebugDebtMenu() {
        runtime.popupService().show(
                text("game.debug.debt.title"),
                new ButtonProps(text("game.debug.debt.50"), () -> debugStartDebtScenario(50)),
                new ButtonProps(text("game.debug.debt.100"), () -> debugStartDebtScenario(100)),
                new ButtonProps(text("game.debug.debt.200"), () -> debugStartDebtScenario(200)),
                new ButtonProps(text("game.debug.debt.500"), () -> debugStartDebtScenario(500)),
                new ButtonProps(text("game.button.retryDebt"), this::retryPendingDebtPayment)
        );
    }

    private void openDebugJailMenu() {
        runtime.popupService().show(
                text("game.debug.jail.title"),
                new ButtonProps(text("game.debug.jail.send"), this::debugSendCurrentPlayerToJail),
                new ButtonProps(text("game.debug.jail.oneRound"), () -> debugSetCurrentPlayerJailRounds(1)),
                new ButtonProps(text("game.debug.jail.threeRounds"), () -> debugSetCurrentPlayerJailRounds(JailSpot.JAIL_ROUND_NUMBER)),
                new ButtonProps(text("game.debug.jail.release"), this::debugReleaseCurrentPlayerFromJail)
        );
    }

    private void openDebugScenarioMenu() {
        runtime.popupService().show(
                text("game.debug.scenario.title"),
                new ButtonProps(text("game.debug.scenario.brownMonopoly"), this::debugGiveBrownMonopoly),
                new ButtonProps(text("game.debug.scenario.brownDebt"), this::debugSetupBrownDebtScenario),
                new ButtonProps(text("game.debug.scenario.railDebt"), this::debugSetupRailroadDebtScenario),
                new ButtonProps(text("game.debug.scenario.resetUi"), this::debugResetTurnState)
        );
    }

    private void debugAdjustCurrentPlayerMoney(int delta) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        int targetMoney = Math.max(0, turnPlayer.getMoneyAmount() + delta);
        debugSetCurrentPlayerMoney(targetMoney);
    }

    private void debugSetCurrentPlayerMoney(int targetMoney) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        turnPlayer.addMoney(targetMoney - turnPlayer.getMoneyAmount());
        runtime.popupService().show(text("game.debug.money.nowHas", turnPlayer.getName(), turnPlayer.getMoneyAmount()));
    }

    private void debugMoveCurrentPlayerTo(SpotType spotType) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        Spot targetSpot = board.getPathWithCriteria(spotType);
        if (turnPlayer.isInJail()) {
            JailSpot.jailTimeLeftMap.remove(turnPlayer);
        }
        turnPlayer.setSpot(targetSpot);
        turnPlayer.setCoords(targetSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.move.moved", turnPlayer.getName(), spotType.name()));
    }

    private void debugStartDebtScenario(int amount) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        log.debug("Debug action: start custom debt scenario for {} amount={}", turnPlayer.getName(), amount);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, amount, text("game.debug.reason.tax", amount)),
                this::restoreNormalTurnControls
        );
    }

    private void debugSetCurrentPlayerJailRounds(int roundsLeft) {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        Spot jailSpot = board.getPathWithCriteria(SpotType.JAIL);
        JailSpot.jailTimeLeftMap.put(turnPlayer, roundsLeft);
        turnPlayer.setSpot(jailSpot);
        turnPlayer.setCoords(jailSpot.getTokenCoords(turnPlayer));
        runtime.popupService().show(text("game.debug.jail.rounds", turnPlayer.getName(), roundsLeft));
    }

    private void debugReleaseCurrentPlayerFromJail() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        JailSpot.jailTimeLeftMap.remove(turnPlayer);
        runtime.popupService().show(text("game.debug.jail.released", turnPlayer.getName()));
    }

    private void debugGiveBrownMonopoly() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugAssignProperty(turnPlayer, SpotType.B1);
        debugAssignProperty(turnPlayer, SpotType.B2);
        runtime.popupService().show(text("game.debug.scenario.brownOwned", turnPlayer.getName()));
    }

    private void debugSetupBrownDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugGiveBrownMonopoly();
        StreetProperty b1 = (StreetProperty) PropertyFactory.getProperty(SpotType.B1);
        StreetProperty b2 = (StreetProperty) PropertyFactory.getProperty(SpotType.B2);
        debugSetCurrentPlayerMoney(1500);
        b1.buyHouses(2);
        b2.buyHouses(2);
        debugSetCurrentPlayerMoney(50);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 300, text("game.debug.reason.brownDebt")),
                this::restoreNormalTurnControls
        );
    }

    private void debugSetupRailroadDebtScenario() {
        Player turnPlayer = players.getTurn();
        if (turnPlayer == null) {
            return;
        }
        debugAssignProperty(turnPlayer, SpotType.RR1);
        debugSetCurrentPlayerMoney(40);
        handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 200, text("game.debug.reason.railDebt")),
                this::restoreNormalTurnControls
        );
    }

    private void debugAssignProperty(Player player, SpotType spotType) {
        Property property = PropertyFactory.getProperty(spotType);
        Player previousOwner = property.getOwnerPlayer();
        if (previousOwner != null && previousOwner != player) {
            previousOwner.removeOwnedProperty(property);
        }
        property.setMortgaged(false);
        player.addOwnedProperty(property);
    }

    private void restoreNormalTurnControls() {
        log.trace("Restoring normal turn controls");
        debtState = null;
        updateDebtButtons();
        showRollDiceControl();
    }

    private void showRollDiceControl() {
        DICES.reset();
        DICES.show();
        endRoundButton.hide();
    }

    private void showEndTurnControl() {
        DICES.hide();
        endRoundButton.show();
    }

    private void hidePrimaryTurnControls() {
        DICES.hide();
        endRoundButton.hide();
    }

    private void enforcePrimaryTurnControlInvariant() {
        if (debtState != null) {
            hidePrimaryTurnControls();
            return;
        }
        if (endRoundButton.isVisible() && DICES.isVisible()) {
            log.warn("Primary turn controls were both visible. Hiding roll dice button to keep end-turn state authoritative.");
            DICES.hide();
        }
    }

    private final class GameComputerTurnContext implements ComputerTurnContext {
        private final Player player;

        private GameComputerTurnContext(Player player) {
            this.player = player;
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
            return runtime.popupService().resolveForComputer(player.getComputerProfile());
        }

        @Override
        public boolean acceptActivePopup() {
            return runtime.popupService().triggerPrimaryAction();
        }

        @Override
        public boolean declineActivePopup() {
            return runtime.popupService().triggerSecondaryAction();
        }

        @Override
        public boolean sellBuilding(SpotType spotType, int count) {
            Property property = PropertyFactory.getProperty(spotType);
            if (!(property instanceof StreetProperty streetProperty)) {
                return false;
            }
            return streetProperty.sellHouses(count);
        }

        @Override
        public boolean buyBuildingRound(SpotType spotType) {
            Property property = PropertyFactory.getProperty(spotType);
            if (!(property instanceof StreetProperty streetProperty)) {
                return false;
            }
            return streetProperty.buyBuildingRoundsAcrossSet(1);
        }

        @Override
        public boolean toggleMortgage(SpotType spotType) {
            return PropertyFactory.getProperty(spotType).handleMortgaging();
        }

        @Override
        public void retryPendingDebtPayment() {
            Game.this.retryPendingDebtPayment();
        }

        @Override
        public void declareBankruptcy() {
            Game.this.declareBankruptcy();
        }

        @Override
        public void rollDice() {
            DICES.rollDice();
        }

        @Override
        public void endTurn() {
            Game.this.endRound(true);
        }
    }

    GameView createGameView(Player currentPlayer) {
        PopupView popupView = runtime.popupService().isAnyVisible()
                ? new PopupView(
                runtime.popupService().activePopupKind(),
                runtime.popupService().activePopupMessage(),
                runtime.popupService().activePopupActions(),
                createPopupPropertyView(currentPlayer)
        )
                : null;
        DebtView debtView = debtState == null ? null : new DebtView(
                debtState.paymentRequest().amount(),
                debtState.paymentRequest().reason(),
                debtState.bankruptcyRisk(),
                debtState.paymentRequest().target().getClass().getSimpleName(),
                debtTargetName(debtState.paymentRequest())
        );
        return new GameView(
                currentPlayer.getId(),
                players.getPlayers().stream()
                        .map(this::createPlayerView)
                        .sorted(Comparator.comparingInt(PlayerView::turnNumber))
                        .toList(),
                new VisibleActionsView(
                        runtime.popupService().isAnyVisible(),
                        retryDebtButton.isVisible(),
                        declareBankruptcyButton.isVisible(),
                        DICES.isVisible(),
                        endRoundButton.isVisible()
                ),
                popupView,
                debtView,
                countUnownedProperties(),
                StreetProperty.BANK_HOUSE_SUPPLY - players.getTotalHouseCount(),
                StreetProperty.BANK_HOTEL_SUPPLY - players.getTotalHotelCount()
        );
    }

    PlayerView createPlayerView(Player player) {
        List<PropertyView> ownedProperties = player.getOwnedProperties().stream()
                .map(property -> createPropertyView(player, property))
                .sorted(Comparator.comparing(property -> property.spotType().ordinal()))
                .toList();
        List<StreetType> completedSets = player.getOwnedProperties().stream()
                .map(Property::getSpotType)
                .map(spotType -> spotType.streetType)
                .distinct()
                .filter(player::ownsAllStreetProperties)
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
        return new PlayerView(
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
            return switch (owner.getOwnedProperties(property.getSpotType().streetType).size()) {
                case 2 -> 70;
                default -> 28;
            };
        }
        Player nonOwner = players.getPlayers().stream()
                .filter(candidate -> candidate != owner)
                .findFirst()
                .orElse(null);
        return nonOwner == null ? 0 : property.getRent(nonOwner);
    }

    private int calculateBoardDangerScore(Player player) {
        return board.getSpots().stream()
                .filter(PropertySpot.class::isInstance)
                .map(PropertySpot.class::cast)
                .map(PropertySpot::getProperty)
                .filter(Property::hasOwner)
                .filter(property -> property.isNotOwner(player))
                .mapToInt(property -> estimateRent(property, property.getOwnerPlayer()))
                .sum();
    }

    private int countUnownedProperties() {
        return (int) board.getSpots().stream()
                .filter(PropertySpot.class::isInstance)
                .map(PropertySpot.class::cast)
                .map(PropertySpot::getProperty)
                .filter(property -> !property.hasOwner())
                .count();
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
                currentPlayer != null && currentPlayer.getOwnedProperties(offeredProperty.getSpotType().streetType).size() + 1
                        >= SpotType.getNumberOfSpots(offeredProperty.getSpotType().streetType)
        );
    }

    private int estimateOfferedPropertyRent(Property property, Player currentPlayer) {
        if (property.getSpotType().streetType.placeType == PlaceType.UTILITY) {
            int utilityCount = currentPlayer == null ? 0 : currentPlayer.getOwnedProperties(property.getSpotType().streetType).size() + 1;
            return utilityCount >= 2 ? 70 : 28;
        }
        Player simulatedVisitor = players.getPlayers().stream()
                .filter(candidate -> candidate != currentPlayer)
                .findFirst()
                .orElse(null);
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
