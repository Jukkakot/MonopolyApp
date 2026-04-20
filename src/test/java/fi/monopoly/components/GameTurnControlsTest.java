package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.dices.Dice;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.support.TestGameSessions;
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

    private static MonopolyButton getRetryDebtButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "retryDebtButton");
    }

    private static MonopolyButton getDeclareBankruptcyButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "declareBankruptcyButton");
    }

    private static MonopolyButton getTradeButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "tradeButton");
    }

    private static MonopolyButton getSaveButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "saveButton");
    }

    private static MonopolyButton getLoadButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "loadButton");
    }

    private static MonopolyButton getBotSpeedButton(Game game) throws ReflectiveOperationException {
        return getButton(game, "botSpeedButton");
    }

    private static MonopolyButton getPlayersButton(Players players, String fieldName) throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (MonopolyButton) field.get(players);
    }

    private static int getPlayersIntField(Players players, String fieldName) throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getInt(players);
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

    private static GameSessionState getSessionState(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("sessionState");
        field.setAccessible(true);
        return (GameSessionState) field.get(game);
    }

    private static fi.monopoly.presentation.game.desktop.runtime.DebugController getDebugController(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("debugController");
        field.setAccessible(true);
        return (fi.monopoly.presentation.game.desktop.runtime.DebugController) field.get(game);
    }

    private static void openGodModeMenu(Game game) throws ReflectiveOperationException {
        getDebugController(game).openGodModeMenu();
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

    private static void refreshButtonInteractivityState(Game game) throws ReflectiveOperationException {
        var method = Game.class.getDeclaredMethod("refreshButtonInteractivityState");
        method.setAccessible(true);
        method.invoke(game);
    }

    private static void dispatchKey(MonopolyRuntime runtime, char key) {
        runtime.eventBus().sendConsumableEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    private static void configureSingleHumanTurn(Game game, MonopolyRuntime runtime) throws ReflectiveOperationException {
        Spot spot = game.getBoard().getSpots().get(0);
        Players previousPlayers = game.players();
        if (previousPlayers != null) {
            previousPlayers.dispose();
        }
        Players players = new Players(runtime);
        players.addPlayer(new Player(runtime, "Human", Color.MEDIUMPURPLE, spot, ComputerPlayerProfile.HUMAN));
        Field playersField = Game.class.getDeclaredField("players");
        playersField.setAccessible(true);
        playersField.set(game, players);
        TestGameSessions.install(runtime, players, game.dices(), game.animations());
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
        game.dices().show();
        endRoundButton.show();

        invokePrimaryControlInvariant(game);

        assertTrue(endRoundButton.isVisible());
        assertFalse(game.dices().isVisible());
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

        assertEquals(1, runtime.popupService().recentPopupMessages().size());
        assertTrue(runtime.popupService().recentPopupMessages().get(0).endsWith("First popup"));
        assertTrue(runtime.popupService().recentPopupMessages().get(0).contains(": "));

        dispatchKey(runtime, '1');
        runtime.eventBus().flushPendingChanges();

        assertEquals(2, runtime.popupService().recentPopupMessages().size());
        assertTrue(runtime.popupService().recentPopupMessages().get(0).endsWith("Second popup"));
        assertTrue(runtime.popupService().recentPopupMessages().get(1).endsWith("First popup"));
    }

    @Test
    void godModeControllerMenuCanSwitchCurrentPlayerBetweenHumanSmokeTestAndStrong() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Player turnPlayer = game.players().getTurn();
        assertNotNull(turnPlayer);
        assertEquals(ComputerPlayerProfile.STRONG, turnPlayer.getComputerProfile());

        openGodModeMenu(game);
        assertTrue(runtime.popupService().triggerPrimaryComputerAction());
        assertTrue(runtime.popupService().activePopupMessage().contains("Strong"));
        assertTrue(runtime.popupService().triggerPrimaryComputerAction());
        assertEquals(ComputerPlayerProfile.HUMAN, turnPlayer.getComputerProfile());
        assertTrue(runtime.popupService().activePopupMessage().contains("Human"));
        assertTrue(runtime.popupService().triggerPrimaryAction());

        openGodModeMenu(game);
        assertTrue(runtime.popupService().triggerPrimaryAction());
        dispatchKey(runtime, '2');
        runtime.eventBus().flushPendingChanges();
        assertEquals(ComputerPlayerProfile.SMOKE_TEST, turnPlayer.getComputerProfile());
        assertTrue(runtime.popupService().activePopupMessage().contains("Smoke"));
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
        MonopolyButton saveButton = getSaveButton(game);
        MonopolyButton loadButton = getLoadButton(game);
        MonopolyButton languageButton = getLanguageButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(game.dices());
        float historyY = invokeFloatMethod(game, "getSidebarHistoryPanelY");
        float historyHeight = invokeFloatMethod(game, "getSidebarHistoryHeight");
        float primaryControlRowY = historyY + historyHeight + 12f;

        assertTrue(endRoundButton.getPosition()[0] >= 16f);
        assertTrue(pauseButton.getPosition()[0] >= 0f);
        assertTrue(tradeButton.getPosition()[0] < pauseButton.getPosition()[0]);
        assertTrue(pauseButton.getPosition()[0] < saveButton.getPosition()[0]);
        assertTrue(saveButton.getPosition()[0] < loadButton.getPosition()[0]);
        assertTrue(languageButton.getPosition()[1] + languageButton.getHeight() <= runtime.app().height);
        boolean usesSidebarBottomRows = Math.abs(pauseButton.getPosition()[1] - primaryControlRowY) < 0.0001f;
        if (usesSidebarBottomRows) {
            assertEquals(primaryControlRowY, pauseButton.getPosition()[1], 0.0001f);
            assertEquals(primaryControlRowY, tradeButton.getPosition()[1], 0.0001f);
            assertEquals(primaryControlRowY, saveButton.getPosition()[1], 0.0001f);
            assertEquals(primaryControlRowY, loadButton.getPosition()[1], 0.0001f);
            assertEquals(primaryControlRowY + languageButton.getHeight() + 8f, languageButton.getPosition()[1], 0.0001f);
            assertTrue(dicePair.getKey().getCoords().x() >= metrics.sidebarX());
            assertTrue(dicePair.getValue().getCoords().x() <= metrics.sidebarRight() - 16);
            assertTrue(dicePair.getKey().getCoords().y() > 206f);
        } else {
            assertEquals(156f, pauseButton.getPosition()[1], 0.0001f);
            assertEquals(156f, tradeButton.getPosition()[1], 0.0001f);
            assertEquals(156f, saveButton.getPosition()[1], 0.0001f);
            assertEquals(156f, loadButton.getPosition()[1], 0.0001f);
            assertEquals(200f, languageButton.getPosition()[1], 0.0001f);
            assertTrue(dicePair.getKey().getCoords().x() <= runtime.app().width);
            assertTrue(dicePair.getValue().getCoords().x() <= runtime.app().width);
        }
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
        Pair<Dice, Dice> dicePair = getDicePair(game.dices());
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
        MonopolyButton saveButton = getSaveButton(game);
        MonopolyButton loadButton = getLoadButton(game);
        MonopolyButton languageButton = getLanguageButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(game.dices());

        assertEquals(16f, endRoundButton.getPosition()[0], 0.0001f);
        assertEquals(16f, endRoundButton.getPosition()[1], 0.0001f);
        assertEquals(156f, pauseButton.getPosition()[1], 0.0001f);
        assertEquals(156f, tradeButton.getPosition()[1], 0.0001f);
        assertEquals(156f, saveButton.getPosition()[1], 0.0001f);
        assertEquals(156f, loadButton.getPosition()[1], 0.0001f);
        assertTrue(pauseButton.getPosition()[0] >= 16f);
        assertTrue(tradeButton.getPosition()[0] >= 16f);
        assertTrue(pauseButton.getPosition()[0] < saveButton.getPosition()[0]);
        assertTrue(saveButton.getPosition()[0] < loadButton.getPosition()[0]);
        assertTrue(languageButton.getPosition()[0] + languageButton.getWidth() <= runtime.app().width);
        assertEquals(200f, languageButton.getPosition()[1], 0.0001f);
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
        assertEquals("trade", runtime.popupService().activePopupKind());
    }

    @Test
    void rollDiceButtonHidesWhilePopupIsVisible() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        configureSingleHumanTurn(game, runtime);

        assertTrue(game.dices().isVisible());

        runtime.popupService().show("Test popup");
        updateSidebarControlPositions(game);

        assertFalse(game.dices().isVisible(), "Roll dice button should never remain above an active popup");

        runtime.popupService().triggerPrimaryAction();
        updateSidebarControlPositions(game);

        assertTrue(game.dices().isVisible(), "Roll dice button should return after popup closes when rolling is still allowed");
    }

    @Test
    void rollDiceButtonDoesNotReturnAfterPopupClosesIfTurnAlreadyRolled() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        configureSingleHumanTurn(game, runtime);

        assertTrue(game.dices().isVisible());

        game.dices().rollDice();
        assertFalse(game.dices().isVisible(), "Roll dice button must hide immediately after rolling");

        runtime.popupService().show("Follow-up popup");
        updateSidebarControlPositions(game);
        assertFalse(game.dices().isVisible(), "Roll dice button must stay hidden while popup is visible");

        runtime.popupService().triggerPrimaryAction();
        updateSidebarControlPositions(game);

        assertFalse(game.dices().isVisible(),
                "Roll dice button must not reappear after popup closes once the turn has already rolled");
    }

    @Test
    void endTurnButtonStaysLeftOfDiceDisplayArea() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        updateSidebarControlPositions(game);

        MonopolyButton endRoundButton = getEndRoundButton(game);
        Pair<Dice, Dice> dicePair = getDicePair(game.dices());
        float firstDiceLeft = dicePair.getKey().getCoords().x() - dicePair.getKey().getUnScaledWidth() / 2f;

        assertTrue(endRoundButton.getPosition()[0] + endRoundButton.getWidth() < firstDiceLeft,
                "End turn button should not overlap the dice display area");
    }

    @Test
    void botTurnKeepsPrimaryTurnControlsHidden() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        game.players().switchTurn();
        invokeShowRollDiceControl(game);
        assertFalse(game.dices().isVisible(), "Roll dice must stay hidden on bot turns");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn must stay hidden on bot turns");

        invokeShowEndTurnControl(game);
        assertFalse(game.dices().isVisible(), "Roll dice must stay hidden on bot turns");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn must stay hidden on bot turns");
    }

    @Test
    void botTurnIgnoresManualPopupAdvanceKeys() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
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
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
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
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
        runtime.popupService().show("Bot popup");
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());
        assertTrue(runtime.popupService().triggerPrimaryComputerAction());
        assertFalse(runtime.popupService().isAnyVisible(),
                "Explicit computer trigger should resolve popup without using manual action path");
    }

    @Test
    void botTurnManualDecisionPopupWaitsForHumanInput() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
        final boolean[] accepted = {false};
        runtime.popupService().showManualDecision(
                "Auction prompt",
                new fi.monopoly.components.popup.components.ButtonProps("Bid M10", () -> accepted[0] = true),
                new fi.monopoly.components.popup.components.ButtonProps("Pass", () -> accepted[0] = false)
        );
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());
        assertFalse(runtime.popupService().resolveForComputer(ComputerPlayerProfile.STRONG),
                "Computer channel must not auto-resolve a human-only decision popup");

        dispatchKey(runtime, '1');
        runtime.eventBus().flushPendingChanges();

        assertTrue(accepted[0], "Manual input should still work for a human auction decision during a bot turn");
        assertFalse(runtime.popupService().isAnyVisible());
    }

    @Test
    void botTurnBlocksGameAffectingButtonsButAllowsLanguageButton() throws ReflectiveOperationException {
        resetNextPlayerId();
        fi.monopoly.text.UiTexts.setLocale(Locale.forLanguageTag("fi"));
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();

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
    void persistentSaveAndLoadButtonsAreVisibleDuringNormalPlay() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        updateDebugButtons(game);

        assertTrue(getSaveButton(game).isVisible());
        assertTrue(getLoadButton(game).isVisible());
    }

    @Test
    void saveAndLoadButtonsRemainUsableDuringBotTurn() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
        updateDebugButtons(game);

        assertTrue(getSaveButton(game).isVisible());
        assertTrue(getLoadButton(game).isVisible());
        getSaveButton(game).pressButton();
        getLoadButton(game).pressButton();
    }

    @Test
    void pauseButtonRemainsUsableDuringBotTurn() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();

        getPauseButton(game).pressButton();

        assertTrue(getSessionState(game).paused(), "Pause button should still work during a bot turn");
    }

    @Test
    void debugModeAllowsTradeButtonDuringBotTurn() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.DEBUG_MODE = true;
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
        updateDebugButtons(game);

        getTradeButton(game).pressButton();

        assertTrue(runtime.popupService().isAnyVisible());
        assertEquals("trade", runtime.popupService().activePopupKind());
    }

    @Test
    void disabledButtonTurnsGrayAndDoesNotOpenActionOutsideDebugMode() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        game.players().switchTurn();
        updateDebugButtons(game);
        refreshButtonInteractivityState(game);

        MonopolyButton tradeButton = getTradeButton(game);
        assertEquals(0xff9f9f9f, tradeButton.currentBackgroundColor());
        assertEquals(0xff9f9f9f, tradeButton.currentForegroundColor());
        assertEquals(0xff9f9f9f, tradeButton.currentActiveColor());

        tradeButton.pressButton();
        assertFalse(runtime.popupService().isAnyVisible());
    }

    @Test
    void enabledButtonKeepsNormalColorsWhenInteractionIsAllowed() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        refreshButtonInteractivityState(game);

        MonopolyButton pauseButton = getPauseButton(game);
        assertNotEquals(0xff9f9f9f, pauseButton.currentBackgroundColor());
        assertNotEquals(0xff9f9f9f, pauseButton.currentForegroundColor());
        assertNotEquals(0xff9f9f9f, pauseButton.currentActiveColor());
    }

    @Test
    void godModeMenusRemainUsableDuringBotTurnWithoutGlobalDebugOverride() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Player bot = game.players().switchTurn();
        assertTrue(bot.isComputerControlled());

        openGodModeMenu(game);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible());
        assertTrue(runtime.popupService().triggerPrimaryAction());
        runtime.eventBus().flushPendingChanges();
        dispatchKey(runtime, '2');
        runtime.eventBus().flushPendingChanges();

        assertEquals(ComputerPlayerProfile.SMOKE_TEST, bot.getComputerProfile());
        assertTrue(runtime.popupService().activePopupMessage().contains("Smoke"));
    }

    @Test
    void botTurnPopupTextUsesPlayerNameInsteadOfSecondPerson() throws ReflectiveOperationException {
        resetNextPlayerId();
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Player bot = game.players().switchTurn();
        runtime.popupService().show("Arrived at WATER WORKS. Do you want to buy it for M150?");
        runtime.eventBus().flushPendingChanges();

        assertEquals(
                bot.getName() + " arrived at WATER WORKS. Does " + bot.getName() + " want to buy it for M150?",
                runtime.popupService().activePopupMessage()
        );
        assertTrue(runtime.popupService().recentPopupMessages().get(0).contains(bot.getName() + ":"));
    }

    @Test
    void deedPagerButtonsRemainUsableDuringBotTurn() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        Player bot = game.players().switchTurn();
        TestGameSessions.install(runtime, game.players(), game.dices(), game.animations());
        game.players().focusPlayer(bot);
        for (fi.monopoly.types.SpotType spotType : java.util.List.of(
                fi.monopoly.types.SpotType.B1,
                fi.monopoly.types.SpotType.B2,
                fi.monopoly.types.SpotType.LB1,
                fi.monopoly.types.SpotType.LB2,
                fi.monopoly.types.SpotType.LB3,
                fi.monopoly.types.SpotType.P1,
                fi.monopoly.types.SpotType.P2,
                fi.monopoly.types.SpotType.P3,
                fi.monopoly.types.SpotType.RR1
        )) {
            fi.monopoly.support.TestObjectFactory.giveProperty(bot,
                    fi.monopoly.components.properties.PropertyFactory.getProperty(spotType));
        }

        MonopolyButton nextDeedsButton = getPlayersButton(game.players(), "nextDeedsButton");
        var updatePagerMethod = Players.class.getDeclaredMethod("updateDeedPagerButtons", fi.monopoly.utils.Coordinates.class, int.class);
        updatePagerMethod.setAccessible(true);
        updatePagerMethod.invoke(game.players(), new fi.monopoly.utils.Coordinates(0, 0), bot.getOwnedProperties().size());

        nextDeedsButton.pressButton();
        assertTrue(getPlayersIntField(game.players(), "deedPageStartIndex") > 0,
                "Deed pager buttons should remain usable during a bot turn");
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

    @Test
    void botSpeedButtonCyclesModeWithoutPopup() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyApp.DEBUG_MODE = true;
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        MonopolyButton botSpeedButton = getBotSpeedButton(game);
        assertTrue(botSpeedButton.getLabel().contains("Normal") || botSpeedButton.getLabel().contains("Normaali"));

        botSpeedButton.pressButton();
        runtime.eventBus().flushPendingChanges();
        assertTrue(botSpeedButton.getLabel().contains("Fast") || botSpeedButton.getLabel().contains("Nopea"));

        botSpeedButton.pressButton();
        runtime.eventBus().flushPendingChanges();
        assertTrue(botSpeedButton.getLabel().contains("Instant") || botSpeedButton.getLabel().contains("Valiton"));

        assertFalse(runtime.popupService().isAnyVisible());
    }

    @Test
    void playersSharingSpotGetDistinctTokenCoordinates() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);
        runtime.eventBus().flushPendingChanges();

        List<Player> players = game.players().getPlayers();
        assertTrue(players.size() >= 2);

        assertNotEquals(players.get(0).getCoords(), players.get(1).getCoords(),
                "Players on the same spot must not collapse to the same token coordinates");
    }

    @Test
    void restoredGameReopensPendingPropertyPurchaseDecision() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                3L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB")),
                List.of(new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of())),
                List.of(),
                new TurnState("player-1", TurnPhase.WAITING_FOR_DECISION, false, false),
                new PendingDecision(
                        "decision-1",
                        DecisionType.PROPERTY_PURCHASE,
                        "player-1",
                        List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY),
                        "Buy WATER WORKS?",
                        new PropertyPurchaseDecisionPayload("U2", "WATER WORKS", 150)
                ),
                null,
                null,
                null,
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertEquals("Eka", game.players().getTurn().getName());
        assertNotNull(game.projectedSessionState().pendingDecision());
        assertTrue(runtime.popupService().isAnyVisible());
        assertEquals("Buy WATER WORKS?", runtime.popupService().activePopupMessage());
        assertFalse(game.dices().isVisible(), "Roll dice should stay hidden while restored decision is pending");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn should stay hidden while restored decision is pending");
    }

    @Test
    void restoredPropertyPurchaseDecisionCanBeAcceptedAndContinuesGame() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                3L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB")),
                List.of(new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of())),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("U2", null, false, 0, 0)),
                new TurnState("player-1", TurnPhase.WAITING_FOR_DECISION, false, false),
                new PendingDecision(
                        "decision-1",
                        DecisionType.PROPERTY_PURCHASE,
                        "player-1",
                        List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY),
                        "Buy WATER WORKS?",
                        new PropertyPurchaseDecisionPayload("U2", "WATER WORKS", 150)
                ),
                null,
                null,
                null,
                new fi.monopoly.domain.session.TurnContinuationState(
                        "continuation-property",
                        "player-1",
                        fi.monopoly.domain.session.TurnContinuationType.RESUME_TURN_FOLLOW_UP,
                        fi.monopoly.domain.session.TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                        "U2",
                        "resume-turn-follow-up"
                ),
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().triggerPrimaryAction());
        runtime.eventBus().flushPendingChanges();

        assertFalse(runtime.popupService().isAnyVisible());
        assertEquals(game.players().getTurn(), fi.monopoly.components.properties.PropertyFactory.getProperty(fi.monopoly.types.SpotType.U2).getOwnerPlayer());
        assertTrue(getEndRoundButton(game).isVisible(), "End turn should become available after restored property purchase resolves");
    }

    @Test
    void restoredGameReattachesDebtControlsFromAuthoritativeSession() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                4L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 40, 1, false, false, false, 0, 0, List.of("B1")),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1200, 3, false, false, false, 0, 0, List.of())
                ),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("B1", "player-1", false, 0, 0)),
                new TurnState("player-1", TurnPhase.RESOLVING_DEBT, false, false),
                null,
                null,
                new DebtStateModel(
                        "debt-1",
                        "player-1",
                        DebtCreditorType.PLAYER,
                        "player-2",
                        200,
                        "Pay rent",
                        true,
                        40,
                        120,
                        List.of(
                                DebtAction.PAY_DEBT_NOW,
                                DebtAction.MORTGAGE_PROPERTY,
                                DebtAction.SELL_BUILDING,
                                DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET,
                                DebtAction.DECLARE_BANKRUPTCY
                        )
                ),
                null,
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(getRetryDebtButton(game).isVisible(), "Retry debt button should reopen from restored debt state");
        assertTrue(getDeclareBankruptcyButton(game).isVisible(), "Bankruptcy button should reflect restored bankruptcy risk");
        assertFalse(game.dices().isVisible(), "Roll dice should stay hidden while restored debt is active");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn should stay hidden while restored debt is active");
    }

    @Test
    void restoredDebtStateCanBeResolvedThroughDebtButton() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                4L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 300, 1, false, false, false, 0, 0, List.of()),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1200, 3, false, false, false, 0, 0, List.of())
                ),
                List.of(),
                new TurnState("player-1", TurnPhase.RESOLVING_DEBT, false, false),
                null,
                null,
                new DebtStateModel(
                        "debt-1",
                        "player-1",
                        DebtCreditorType.PLAYER,
                        "player-2",
                        200,
                        "Pay rent",
                        false,
                        300,
                        0,
                        List.of(DebtAction.PAY_DEBT_NOW)
                ),
                null,
                new fi.monopoly.domain.session.TurnContinuationState(
                        "continuation-debt",
                        "player-1",
                        fi.monopoly.domain.session.TurnContinuationType.RESUME_AFTER_DEBT,
                        fi.monopoly.domain.session.TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                        null,
                        "resume-after-debt"
                ),
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        getRetryDebtButton(game).pressButton();
        runtime.eventBus().flushPendingChanges();
        runtime.popupService().triggerPrimaryAction();
        runtime.eventBus().flushPendingChanges();

        assertFalse(getRetryDebtButton(game).isVisible());
        assertFalse(getDeclareBankruptcyButton(game).isVisible());
        assertTrue(getEndRoundButton(game).isVisible(), "End turn should become available after restored debt resolves");
    }

    @Test
    void restoredGameReopensAuctionPopupFromAuthoritativeSession() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                5L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of()),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1400, 0, false, false, false, 0, 0, List.of())
                ),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("B1", null, false, 0, 0)),
                new TurnState("player-1", TurnPhase.WAITING_FOR_AUCTION, false, false),
                null,
                new AuctionState(
                        "auction-1",
                        "B1",
                        "player-1",
                        "player-1",
                        "player-2",
                        70,
                        80,
                        java.util.Set.of(),
                        List.of("player-1", "player-2"),
                        AuctionStatus.ACTIVE,
                        0,
                        null
                ),
                null,
                null,
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible(), "Auction popup should reopen from restored auction state");
        assertEquals("propertyAuction", runtime.popupService().activePopupKind());
        assertFalse(game.dices().isVisible(), "Roll dice should stay hidden while restored auction is active");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn should stay hidden while restored auction is active");
    }

    @Test
    void restoredAuctionCanAdvanceAndResolve() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                5L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of()),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1400, 0, false, false, false, 0, 0, List.of())
                ),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("B1", null, false, 0, 0)),
                new TurnState("player-1", TurnPhase.WAITING_FOR_AUCTION, false, false),
                null,
                new AuctionState(
                        "auction-1",
                        "B1",
                        "player-1",
                        "player-1",
                        "player-2",
                        70,
                        80,
                        java.util.Set.of(),
                        List.of("player-1", "player-2"),
                        AuctionStatus.ACTIVE,
                        0,
                        null
                ),
                null,
                null,
                new fi.monopoly.domain.session.TurnContinuationState(
                        "continuation-auction",
                        "player-1",
                        fi.monopoly.domain.session.TurnContinuationType.RESUME_AFTER_AUCTION,
                        fi.monopoly.domain.session.TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                        "B1",
                        "resume-after-auction"
                ),
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().triggerPrimaryAction());
        runtime.eventBus().flushPendingChanges();
        assertTrue(runtime.popupService().triggerSecondaryAction());
        runtime.eventBus().flushPendingChanges();
        assertTrue(runtime.popupService().triggerPrimaryAction());
        runtime.eventBus().flushPendingChanges();

        assertFalse(runtime.popupService().isAnyVisible());
        assertEquals(game.players().getTurn(), fi.monopoly.components.properties.PropertyFactory.getProperty(fi.monopoly.types.SpotType.B1).getOwnerPlayer());
        assertTrue(getEndRoundButton(game).isVisible(), "End turn should become available after restored auction resolves");
    }

    @Test
    void restoredGameReopensTradePopupFromAuthoritativeSession() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                6L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of("B1")),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1400, 0, false, false, false, 0, 0, List.of())
                ),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("B1", "player-1", false, 0, 0)),
                new TurnState("player-2", TurnPhase.WAITING_FOR_DECISION, false, false),
                null,
                null,
                null,
                new TradeState(
                        "trade-1",
                        "player-1",
                        "player-2",
                        TradeStatus.SUBMITTED,
                        new TradeOfferState(
                                "player-1",
                                "player-2",
                                new TradeSelectionState(0, List.of("B1"), 0),
                                TradeSelectionState.NONE
                        ),
                        "player-1",
                        true,
                        "player-2",
                        "player-1",
                        List.of()
                ),
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().isAnyVisible(), "Trade popup should reopen from restored trade state");
        assertEquals("trade", runtime.popupService().activePopupKind());
        assertFalse(game.dices().isVisible(), "Roll dice should stay hidden while restored trade is active");
        assertFalse(getEndRoundButton(game).isVisible(), "End turn should stay hidden while restored trade is active");
    }

    @Test
    void restoredTradeCanBeAccepted() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState restoredState = new SessionState(
                "local-session",
                6L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.HUMAN, ControlMode.MANUAL, "Toka", "HUMAN", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 0, false, false, false, 0, 0, List.of("B1")),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1400, 0, false, false, false, 0, 0, List.of())
                ),
                List.of(new fi.monopoly.domain.session.PropertyStateSnapshot("B1", "player-1", false, 0, 0)),
                new TurnState("player-2", TurnPhase.WAITING_FOR_DECISION, false, false),
                null,
                null,
                null,
                new TradeState(
                        "trade-1",
                        "player-1",
                        "player-2",
                        TradeStatus.SUBMITTED,
                        new TradeOfferState(
                                "player-1",
                                "player-2",
                                new TradeSelectionState(0, List.of("B1"), 0),
                                TradeSelectionState.NONE
                        ),
                        "player-2",
                        false,
                        "player-2",
                        "player-1",
                        List.of()
                ),
                null
        );

        Game game = new Game(runtime, restoredState);
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.popupService().triggerPrimaryAction());
        runtime.eventBus().flushPendingChanges();

        assertFalse(runtime.popupService().isAnyVisible());
        assertEquals(game.players().getPlayers().get(1), fi.monopoly.components.properties.PropertyFactory.getProperty(fi.monopoly.types.SpotType.B1).getOwnerPlayer());
    }
}
