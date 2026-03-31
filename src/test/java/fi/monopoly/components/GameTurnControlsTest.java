package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.dices.Dice;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.utils.LayoutMetrics;
import javafx.scene.paint.Color;
import javafx.util.Pair;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static processing.event.KeyEvent.PRESS;

class GameTurnControlsTest {

    private static MonopolyRuntime initHeadlessRuntime() {
        return initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
    }

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    private static MonopolyButton getEndRoundButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "endRoundButton");
    }

    private static MonopolyButton getLanguageButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "languageButton");
    }

    private static MonopolyButton getPauseButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "pauseButton");
    }

    private static MonopolyButton getTradeButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "tradeButton");
    }

    private static MonopolyButton getButton(Game game, String fieldName) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (MonopolyButton) field.get(game);
    }

    private static float invokeFloatMethod(Game game, String methodName) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (float) method.invoke(game);
    }

    private static MonopolyButton getDebugGodModeButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "debugGodModeButton");
    }

    @SuppressWarnings("unchecked")
    private static Pair<Dice, Dice> getDicePair(Dices dices) throws ReflectiveOperationException {
        Field field = Dices.class.getDeclaredField("dices");
        field.setAccessible(true);
        return (Pair<Dice, Dice>) field.get(dices);
    }

    private static void resetNextPlayerId() throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("NEXT_ID");
        field.setAccessible(true);
        field.setInt(null, 0);
    }

    private static void invokePrimaryControlInvariant(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("enforcePrimaryTurnControlInvariant");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void updateSidebarControlPositions(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("updateSidebarControlPositions");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void invokeShowRollDiceControl(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("showRollDiceControl");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void invokeShowEndTurnControl(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("showEndTurnControl");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void updateDebugButtons(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("updateDebugButtons");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void dispatchKey(MonopolyRuntime runtime, char key) {
        runtime.eventBus().sendConsumableEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    private static void configureSingleHumanTurn(Game game, MonopolyRuntime runtime) throws ReflectiveOperationException {
        Spot spot = game.board.getSpots().get(0);
        Game.players = new Players(runtime);
        Game.players.addPlayer(new Player(runtime, "Human", Color.MEDIUMPURPLE, spot, ComputerPlayerProfile.HUMAN));
        invokeShowRollDiceControl(game);
        runtime.eventBus().flushPendingChanges();
    }

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void invariantHidesRollDiceWhenEndRoundIsAlreadyVisible() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        MonopolyButton endRoundButton = getEndRoundButton(game);
        Game.DICES.show();
        endRoundButton.show();

        invokePrimaryControlInvariant(game);

        assertTrue(endRoundButton.isVisible());
        assertFalse(Game.DICES.isVisible());
    }

    @Test
    void popupServiceKeepsShownPopupTextsInRecentHistory() {
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        try {
            configureSingleHumanTurn(game, runtime);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        runtime.popupService().show("First popup");
        runtime.popupService().show("Second popup");
        runtime.eventBus().flushPendingChanges();

        assertEquals(List.of("First popup"), runtime.popupService().recentPopupMessages());

        dispatchKey(runtime, '1');
        runtime.eventBus().flushPendingChanges();

        assertEquals(List.of("Second popup", "First popup"), runtime.popupService().recentPopupMessages());
    }

    @Test
    void gameReadsSidebarMetricsFromCurrentWindowSize() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(1200, 800);
        Game game = new Game(runtime);

        var method = Game.class.getDeclaredMethod("getLayoutMetrics");
        method.setAccessible(true);
        LayoutMetrics metrics = (LayoutMetrics) method.invoke(game);

        assertEquals(1200, metrics.windowWidth(), 0.0001f);
        assertEquals(800, metrics.windowHeight(), 0.0001f);
        assertEquals(1200 - fi.monopoly.components.spots.Spot.SPOT_W * 12, metrics.sidebarWidth(), 0.0001f);
    }

    @Test
    void sidebarHistoryShrinksAndContentTopStaysAboveItWhenWindowIsShort() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(1700, 560);
        Game game = new Game(runtime);

        var historyHeightMethod = Game.class.getDeclaredMethod("getSidebarHistoryHeight");
        historyHeightMethod.setAccessible(true);
        float historyHeight = (float) historyHeightMethod.invoke(game);

        var historyYMethod = Game.class.getDeclaredMethod("getSidebarHistoryPanelY");
        historyYMethod.setAccessible(true);
        float historyY = (float) historyYMethod.invoke(game);

        var contentTopMethod = Game.class.getDeclaredMethod("getSidebarContentTop");
        contentTopMethod.setAccessible(true);
        float contentTop = (float) contentTopMethod.invoke(game);

        assertTrue(historyHeight <= 192f);
        assertTrue(historyHeight >= 112f);
        assertTrue(contentTop <= historyY - 16f);
    }

    @Test
    void sidebarControlsAndDiceFollowCurrentWindowSize() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        runtime.app().width = 1200;
        runtime.app().height = 780;
        updateSidebarControlPositions(game);

        LayoutMetrics metrics = LayoutMetrics.fromWindow(runtime.app().width, runtime.app().height);
        MonopolyButton endRoundButton = getEndRoundButton(game);
        MonopolyButton pauseButton = getPauseButton(game);
        MonopolyButton tradeButton = getTradeButton(game);
        MonopolyButton languageButton = getLanguageButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(Game.DICES);
        float historyY = invokeFloatMethod(game, "getSidebarHistoryPanelY");
        float historyHeight = invokeFloatMethod(game, "getSidebarHistoryHeight");

        assertEquals(metrics.sidebarX() + 16, endRoundButton.getPosition()[0], 0.0001f);
        assertEquals(historyY + historyHeight + 12, pauseButton.getPosition()[1], 0.0001f);
        assertEquals(historyY + historyHeight + 12, tradeButton.getPosition()[1], 0.0001f);
        assertTrue(pauseButton.getPosition()[0] >= 0f);
        assertTrue(tradeButton.getPosition()[0] < pauseButton.getPosition()[0]);
        assertEquals(historyY + historyHeight + 12, languageButton.getPosition()[1], 0.0001f);
        assertTrue(dicePair.getKey().getCoords().x() >= metrics.sidebarX());
        assertTrue(dicePair.getValue().getCoords().x() <= metrics.sidebarRight() - 16);
        assertTrue(dicePair.getKey().getCoords().y() > 206f);
    }

    @Test
    void shortDebugSidebarKeepsControlsAboveHistoryPanel() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.DEBUG_MODE = true;
        MonopolyRuntime runtime = initHeadlessRuntime(1700, 560);
        Game game = new Game(runtime);

        updateSidebarControlPositions(game);

        MonopolyButton endRoundButton = getEndRoundButton(game);
        MonopolyButton debugGodModeButton = getDebugGodModeButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(Game.DICES);
        float historyY = invokeFloatMethod(game, "getSidebarHistoryPanelY");

        float lowestDiceBottom = Math.max(
                dicePair.getKey().getCoords().y() + dicePair.getKey().getUnScaledHeight() / 2f,
                dicePair.getValue().getCoords().y() + dicePair.getValue().getUnScaledHeight() / 2f
        );

        assertTrue(endRoundButton.getPosition()[1] < 192f);
        assertTrue(lowestDiceBottom < historyY);
        assertTrue(debugGodModeButton.getPosition()[1] + debugGodModeButton.getHeight() < historyY);
    }

    @Test
    void narrowWindowMovesPrimaryControlsIntoBoardOverlay() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(800, 700);
        Game game = new Game(runtime);

        updateSidebarControlPositions(game);

        MonopolyButton endRoundButton = getEndRoundButton(game);
        MonopolyButton pauseButton = getPauseButton(game);
        MonopolyButton tradeButton = getTradeButton(game);
        MonopolyButton languageButton = getLanguageButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(Game.DICES);

        assertEquals(16f, endRoundButton.getPosition()[0], 0.0001f);
        assertEquals(16f, endRoundButton.getPosition()[1], 0.0001f);
        assertEquals(156f, pauseButton.getPosition()[1], 0.0001f);
        assertEquals(156f, tradeButton.getPosition()[1], 0.0001f);
        assertTrue(pauseButton.getPosition()[0] >= 16f);
        assertTrue(tradeButton.getPosition()[0] >= 16f);
        assertTrue(languageButton.getPosition()[0] + languageButton.getWidth() <= runtime.app().width);
        assertEquals(156f, languageButton.getPosition()[1], 0.0001f);
        assertTrue(dicePair.getKey().getCoords().x() <= runtime.app().width);
        assertTrue(dicePair.getValue().getCoords().x() <= runtime.app().width);
    }

    @Test
    void tradeShortcutOpensTradePartnerPopup() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        assertTrue(game.onEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, 't', 't')));
        assertTrue(runtime.popupService().isAnyVisible());
        assertEquals("TradePopup", runtime.popupService().activePopupKind());
    }

    @Test
    void rollDiceButtonHidesWhilePopupIsVisible() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        configureSingleHumanTurn(game, runtime);

        assertTrue(Game.DICES.isVisible());

        runtime.popupService().show("Test popup");
        updateSidebarControlPositions(game);

        assertFalse(Game.DICES.isVisible(), "Roll dice button should never remain above an active popup");

        runtime.popupService().triggerPrimaryAction();
        updateSidebarControlPositions(game);

        assertTrue(Game.DICES.isVisible(), "Roll dice button should return after popup closes when rolling is still allowed");
    }

    @Test
    void rollDiceButtonDoesNotReturnAfterPopupClosesIfTurnAlreadyRolled() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        configureSingleHumanTurn(game, runtime);

        assertTrue(Game.DICES.isVisible());

        Game.DICES.rollDice();
        assertFalse(Game.DICES.isVisible(), "Roll dice button must hide immediately after rolling");

        runtime.popupService().show("Follow-up popup");
        updateSidebarControlPositions(game);
        assertFalse(Game.DICES.isVisible(), "Roll dice button must stay hidden while popup is visible");

        runtime.popupService().triggerPrimaryAction();
        updateSidebarControlPositions(game);

        assertFalse(Game.DICES.isVisible(),
                "Roll dice button must not reappear after popup closes once the turn has already rolled");
    }

    @Test
    void endTurnButtonStaysLeftOfDiceDisplayArea() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        updateSidebarControlPositions(game);

        MonopolyButton endRoundButton = getEndRoundButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(Game.DICES);
        float firstDiceLeft = dicePair.getKey().getCoords().x() - dicePair.getKey().getUnScaledWidth() / 2f;

        assertTrue(endRoundButton.getPosition()[0] + endRoundButton.getWidth() < firstDiceLeft,
                "End turn button should not overlap the dice display area");
    }

    @Test
    void botTurnKeepsPrimaryTurnControlsHidden() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Game.players.switchTurn();
        invokeShowRollDiceControl(game);
        assertFalse(Game.DICES.isVisible(), "Roll dice must stay hidden on bot turns");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn must stay hidden on bot turns");

        invokeShowEndTurnControl(game);
        assertFalse(Game.DICES.isVisible(), "Roll dice must stay hidden on bot turns");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn must stay hidden on bot turns");
    }

    @Test
    void botTurnIgnoresManualPopupAdvanceKeys() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.players.switchTurn();
        runtime.popupService().show("Bot popup");
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());

        dispatchKey(runtime, '1');
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible(),
                "User input must not close or accept popups during a bot turn");
    }

    @Test
    void botTurnPopupStillResolvesThroughComputerChannel() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.players.switchTurn();
        runtime.popupService().show("Bot popup");
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());
        assertTrue(runtime.popupService().resolveForComputer(ComputerPlayerProfile.STRONG));
        assertFalse(runtime.popupService().isAnyVisible(),
                "Computer popup resolution must remain available during a bot turn");
    }

    @Test
    void botTurnPopupAllowsExplicitComputerPrimaryTrigger() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.players.switchTurn();
        runtime.popupService().show("Bot popup");
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());
        assertTrue(runtime.popupService().triggerPrimaryComputerAction());
        assertFalse(runtime.popupService().isAnyVisible(),
                "Explicit computer trigger should resolve popup without using manual action path");
    }

    @Test
    void botTurnBlocksGameAffectingButtonsButAllowsLanguageButton() throws ReflectiveOperationException {
        resetNextPlayerId();
        fi.monopoly.text.UiTexts.setLocale(Locale.forLanguageTag("fi"));
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Game.players.switchTurn();

        getTradeButton(game).pressButton();
        runtime.eventBus().flushPendingChanges();
        assertFalse(runtime.popupService().isAnyVisible(),
                "Trade button should be blocked during a bot turn");

        getLanguageButton(game).pressButton();
        runtime.eventBus().flushPendingChanges();
        assertEquals(Locale.ENGLISH, fi.monopoly.text.UiTexts.getLocale(),
                "Language button should remain usable during a bot turn");
        assertFalse(runtime.popupService().isAnyVisible(),
                "Language button should not open a popup during a bot turn");
    }

    @Test
    void languageButtonCyclesToNextSupportedLocaleWithoutPopup() throws ReflectiveOperationException {
        resetNextPlayerId();
        fi.monopoly.text.UiTexts.setLocale(Locale.forLanguageTag("fi"));
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        getLanguageButton(game).pressButton();
        runtime.eventBus().flushPendingChanges();

        assertEquals(Locale.ENGLISH, fi.monopoly.text.UiTexts.getLocale());
        assertFalse(runtime.popupService().isAnyVisible(),
                "Language button should switch locale directly without opening a popup");
    }
}
