package fi.monopoly.client.desktop;

import controlP5.ControlP5;
import processing.core.PFont;

/**
 * Holds current desktop runtime UI resources that still need process-wide access during the
 * transitional single-process client architecture.
 */
public final class DesktopRuntimeResources {
    private static ControlP5 controlLayer;
    private static PFont font10;
    private static PFont font20;
    private static PFont font30;

    private DesktopRuntimeResources() {
    }

    public static ControlP5 controlLayer() {
        return controlLayer;
    }

    public static void setControlLayer(ControlP5 controlLayer) {
        DesktopRuntimeResources.controlLayer = controlLayer;
    }

    public static PFont font10() {
        return font10;
    }

    public static PFont font20() {
        return font20;
    }

    public static PFont font30() {
        return font30;
    }

    public static void setFonts(PFont font10, PFont font20, PFont font30) {
        DesktopRuntimeResources.font10 = font10;
        DesktopRuntimeResources.font20 = font20;
        DesktopRuntimeResources.font30 = font30;
    }
}
