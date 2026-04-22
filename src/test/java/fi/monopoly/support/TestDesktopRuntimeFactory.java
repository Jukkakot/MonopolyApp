package fi.monopoly.support;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.DesktopRuntimeResources;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

/**
 * Creates a minimal headless Processing/runtime context for desktop-facing tests.
 */
public final class TestDesktopRuntimeFactory {
    private TestDesktopRuntimeFactory() {
    }

    public static DesktopTestContext create() {
        return create(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
    }

    public static DesktopTestContext create(int width, int height) {
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
        DesktopRuntimeResources.setControlLayer(controlP5);
        DesktopRuntimeResources.setFonts(font, font, font);

        MonopolyRuntime runtime = MonopolyRuntime.initialize(app, controlP5, font, font, font);
        return new DesktopTestContext(app, runtime);
    }

    public record DesktopTestContext(MonopolyApp app, MonopolyRuntime runtime) {
    }
}
