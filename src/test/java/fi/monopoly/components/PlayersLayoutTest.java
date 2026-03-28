package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayersLayoutTest {

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

    private static Object invoke(Players players, String methodName) throws ReflectiveOperationException {
        var method = Players.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(players);
    }

    @Test
    void defaultSidebarKeepsFullDeedPageAndRegularSummaryHeight() throws ReflectiveOperationException {
        Players players = new Players(initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT));

        assertEquals(6, invoke(players, "getDeedsPerRow"));
        assertFalse((boolean) invoke(players, "useCompactSummaryLayout"));
        assertEquals(160, invoke(players, "getTextInfoHeight"));
    }

    @Test
    void narrowSidebarUsesCompactSummaryAndFewerVisibleDeeds() throws ReflectiveOperationException {
        Players players = new Players(initHeadlessRuntime(1200, MonopolyApp.DEFAULT_WINDOW_HEIGHT));

        int deedsPerRow = (int) invoke(players, "getDeedsPerRow");

        assertTrue(deedsPerRow < 5);
        assertTrue(deedsPerRow >= 1);
        assertTrue((boolean) invoke(players, "useCompactSummaryLayout"));
        assertEquals(192, invoke(players, "getTextInfoHeight"));
    }

    @Test
    void widerSidebarShowsMoreThanDefaultFiveDeedsBeforePaging() throws ReflectiveOperationException {
        Players players = new Players(initHeadlessRuntime(1900, MonopolyApp.DEFAULT_WINDOW_HEIGHT));

        int deedsPerRow = (int) invoke(players, "getDeedsPerRow");

        assertTrue(deedsPerRow > 6);
    }
}
