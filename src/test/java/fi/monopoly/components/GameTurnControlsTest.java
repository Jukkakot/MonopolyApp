package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.utils.LayoutMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.lang.reflect.Field;
import java.util.List;

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
        Field field = Game.class.getDeclaredField("endRoundButton");
        field.setAccessible(true);
        return (MonopolyButton) field.get(game);
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

    private static void dispatchKey(MonopolyRuntime runtime, char key) {
        runtime.eventBus().sendConsumableEvent(new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key));
    }

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
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
}
