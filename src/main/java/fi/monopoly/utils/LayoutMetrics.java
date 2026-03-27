package fi.monopoly.utils;

import fi.monopoly.MonopolyApp;
import fi.monopoly.components.spots.Spot;

/**
 * Centralizes measurements derived from the current window size so layout
 * code can gradually move away from hard-coded constants.
 */
public record LayoutMetrics(
        float windowWidth,
        float windowHeight,
        float boardWidth,
        float sidebarX,
        float sidebarWidth
) {
    private static final float DEFAULT_BOARD_WIDTH = Spot.SPOT_W * 12;

    public static LayoutMetrics fromWindow(float windowWidth, float windowHeight) {
        float resolvedWindowWidth = Math.max(0, windowWidth);
        float resolvedWindowHeight = Math.max(0, windowHeight);
        float resolvedBoardWidth = Math.min(DEFAULT_BOARD_WIDTH, resolvedWindowWidth);
        float resolvedSidebarX = resolvedBoardWidth;
        float resolvedSidebarWidth = Math.max(0, resolvedWindowWidth - resolvedBoardWidth);
        return new LayoutMetrics(
                resolvedWindowWidth,
                resolvedWindowHeight,
                resolvedBoardWidth,
                resolvedSidebarX,
                resolvedSidebarWidth
        );
    }

    public static LayoutMetrics defaultWindow() {
        return fromWindow(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
    }

    public float sidebarRight() {
        return sidebarX + sidebarWidth;
    }

    public boolean hasSidebarSpace() {
        return sidebarWidth > 0;
    }
}
