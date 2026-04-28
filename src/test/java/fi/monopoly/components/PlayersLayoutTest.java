package fi.monopoly.components;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.support.TestDesktopRuntimeFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayersLayoutTest {

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        return TestDesktopRuntimeFactory.create(width, height).runtime();
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
