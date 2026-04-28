package fi.monopoly.utils;

import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.components.spots.Spot;

/**
 * Immutable runtime layout derived from the current window size.
 * Static UI design tokens belong in {@link UiTokens}.
 */
public record LayoutMetrics(
        float windowWidth,
        float windowHeight,
        float boardWidth,
        float sidebarX,
        float sidebarWidth
) {
    private static final float DEFAULT_BOARD_WIDTH = Spot.SPOT_W * 12;
    private static final float COMPACT_SIDEBAR_HEIGHT_THRESHOLD = 700;
    private static final float TIGHT_SIDEBAR_HEIGHT_THRESHOLD = 620;

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

    public boolean usesCompactSidebarHeight() {
        return windowHeight < COMPACT_SIDEBAR_HEIGHT_THRESHOLD;
    }

    public boolean usesTightSidebarHeight() {
        return windowHeight < TIGHT_SIDEBAR_HEIGHT_THRESHOLD;
    }

    public float sidebarTitleY() {
        return usesCompactSidebarHeight() ? 28 : 32;
    }

    public float sidebarHeaderRow1Y() {
        return usesCompactSidebarHeight() ? 56 : 64;
    }

    public float sidebarHeaderRow2Y() {
        return sidebarHeaderRow1Y() + (usesCompactSidebarHeight() ? 28 : 32);
    }

    public float sidebarHeaderRow3Y() {
        return sidebarHeaderRow2Y() + (usesCompactSidebarHeight() ? 28 : 32);
    }

    public float sidebarHeaderHeight() {
        return sidebarHeaderRow3Y() + (usesCompactSidebarHeight() ? 24 : 32);
    }

    public float sidebarPrimaryButtonY() {
        return sidebarHeaderHeight() + (usesCompactSidebarHeight() ? 16 : 32);
    }

    public float sidebarDebugButtonRow1Y() {
        return sidebarPrimaryButtonY() + (usesCompactSidebarHeight() ? 56 : 96);
    }

    public float sidebarDebugButtonRow2Y() {
        return sidebarDebugButtonRow1Y() + (usesCompactSidebarHeight() ? 40 : 48);
    }

    public float sidebarDebugButtonRow3Y() {
        return sidebarDebugButtonRow2Y() + (usesCompactSidebarHeight() ? 40 : 48);
    }

    public float sidebarReservedTop(boolean debugMode) {
        return debugMode
                ? sidebarDebugButtonRow3Y() + (usesCompactSidebarHeight() ? 52 : 62)
                : sidebarPrimaryButtonY() + (usesCompactSidebarHeight() ? 72 : 96);
    }

    public float sidebarHistoryBottomMargin() {
        return usesTightSidebarHeight() ? 56 : 80;
    }

    public float debtSectionTitleY() {
        return sidebarPrimaryButtonY() + 48;
    }

    public float debtTextY() {
        return debtSectionTitleY() + 32;
    }
}
