package fi.monopoly.utils;

import fi.monopoly.MonopolyApp;
import fi.monopoly.components.spots.Spot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayoutMetricsTest {

    @Test
    void defaultWindowMatchesCurrentBoardAndSidebarSplit() {
        LayoutMetrics metrics = LayoutMetrics.defaultWindow();

        assertEquals(MonopolyApp.DEFAULT_WINDOW_WIDTH, metrics.windowWidth());
        assertEquals(MonopolyApp.DEFAULT_WINDOW_HEIGHT, metrics.windowHeight());
        assertEquals(Spot.SPOT_W * 12, metrics.boardWidth(), 0.0001f);
        assertEquals(Spot.SPOT_W * 12, metrics.sidebarX(), 0.0001f);
        assertEquals(MonopolyApp.DEFAULT_WINDOW_WIDTH - Spot.SPOT_W * 12, metrics.sidebarWidth(), 0.0001f);
    }

    @Test
    void fixedLayoutMinimumWindowMatchesCurrentSafeBaseline() {
        assertEquals(MonopolyApp.DEFAULT_WINDOW_WIDTH, UiTokens.minimumFixedLayoutWindowWidth());
        assertEquals(MonopolyApp.DEFAULT_WINDOW_HEIGHT, UiTokens.minimumFixedLayoutWindowHeight());
    }

    @Test
    void narrowWindowClampsSidebarToRemainingSpace() {
        LayoutMetrics metrics = LayoutMetrics.fromWindow(1200, 800);

        assertEquals(Spot.SPOT_W * 12, metrics.boardWidth(), 0.0001f);
        assertEquals(1200 - Spot.SPOT_W * 12, metrics.sidebarWidth(), 0.0001f);
        assertTrue(metrics.hasSidebarSpace());
    }

    @Test
    void veryNarrowWindowEliminatesSidebarSpaceInsteadOfGoingNegative() {
        LayoutMetrics metrics = LayoutMetrics.fromWindow(800, 600);

        assertEquals(800, metrics.boardWidth(), 0.0001f);
        assertEquals(800, metrics.sidebarX(), 0.0001f);
        assertEquals(0, metrics.sidebarWidth(), 0.0001f);
        assertEquals(800, metrics.sidebarRight(), 0.0001f);
        assertFalse(metrics.hasSidebarSpace());
    }
}
