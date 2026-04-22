package fi.monopoly.client.desktop;

import fi.monopoly.client.session.desktop.DesktopSessionRenderView;
import fi.monopoly.components.event.MonopolyEventBus;
import fi.monopoly.components.event.MonopolyEventObserver;
import fi.monopoly.utils.UiTokens;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import processing.awt.PSurfaceAWT;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

@Slf4j
public class MonopolyApp extends MonopolyEventObserver implements DesktopRenderingContext {
    public static final int TARGET_FRAME_RATE = 120;
    public static final char ENTER = '\n';
    public static final char SPACE = ' ';
    public static final int DEFAULT_WINDOW_WIDTH = 1700;
    public static final int DEFAULT_WINDOW_HEIGHT = 996;
    private final DesktopAppShell desktopAppShell = new DesktopAppShell(this);
    private int lastDrawWidth = -1;
    private int lastDrawHeight = -1;

    public void settings() {
        size(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT, FX2D);
    }

    public void setup() {
        frameRate(TARGET_FRAME_RATE);
        DesktopImageCatalog.initialize(this);
        configureWindowSizing();
        DesktopRuntimeResources.setFonts(
                createFont("Monopoly Regular.ttf", 10),
                createFont("Monopoly Regular.ttf", 20),
                createFont("Monopoly Regular.ttf", 30)
        );
        desktopAppShell.startFreshSession();
    }

    private void configureWindowSizing() {
        surface.setResizable(true);
        Object nativeSurface = surface.getNative();
        if (nativeSurface instanceof PSurfaceAWT.SmoothCanvas smoothCanvas && smoothCanvas.getFrame() != null) {
            smoothCanvas.getFrame().setMinimumSize(new Dimension(
                    UiTokens.minimumFixedLayoutWindowWidth(),
                    UiTokens.minimumFixedLayoutWindowHeight()
            ));
            smoothCanvas.getFrame().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    log.info("AWT resize event: frame={}x{}, sketch={}x{}",
                            smoothCanvas.getFrame().getWidth(),
                            smoothCanvas.getFrame().getHeight(),
                            width,
                            height
                    );
                }
            });
            return;
        }
        if (nativeSurface instanceof Canvas fxCanvas) {
            Platform.runLater(() -> applyFxMinimumWindowSize(fxCanvas));
        }
    }

    private void applyFxMinimumWindowSize(Canvas fxCanvas) {
        if (fxCanvas.getScene() == null || fxCanvas.getScene().getWindow() == null) {
            log.warn("FX2D canvas scene/window not ready when applying minimum size");
            return;
        }
        Stage stage = (Stage) fxCanvas.getScene().getWindow();
        stage.setMinWidth(UiTokens.minimumFixedLayoutWindowWidth());
        stage.setMinHeight(UiTokens.minimumFixedLayoutWindowHeight());
        log.info("Applied FX minimum window size: {}x{}",
                UiTokens.minimumFixedLayoutWindowWidth(),
                UiTokens.minimumFixedLayoutWindowHeight());
    }

    public void draw() {
        logWindowSizeChangeFromDraw();
        background(205, 230, 209);
        desktopAppShell.advanceFrame();
        DesktopSessionRenderView currentView = desktopAppShell.currentView();
        if (currentView == null) {
            return;
        }
        currentView.draw();
        if (DesktopClientSettings.debugMode()) {
            push();
            fill(22, 36, 31, 190);
            noStroke();
            rect(12, 12, 270, 144, 14);
            fill(245, 245, 245);
            textAlign(LEFT, TOP);
            textFont(DesktopRuntimeResources.font20());
            float debugTextY = 22;
            for (String line : currentView.debugPerformanceLines(frameRate)) {
                text(line, 24, debugTextY);
                debugTextY += 20;
            }
            fill(255, 105, 180);
            noStroke();
            circle(mouseX, mouseY, 20);
            textFont(DesktopRuntimeResources.font20());
            text(fi.monopoly.text.UiTexts.text("app.debug.mouseCoords", mouseX, mouseY), mouseX - 40, mouseY + 30);
            pop();
        }
    }

    public void saveLocalSession() {
        desktopAppShell.saveLocalSession();
    }

    public void loadLocalSession() {
        desktopAppShell.loadLocalSession();
    }

    public void windowResized() {
//        log.debug("Processing windowResized callback: sketch={}x{}", width, height);
    }

    private void logWindowSizeChangeFromDraw() {
        if (width == lastDrawWidth && height == lastDrawHeight) {
            return;
        }
//        log.debug("Draw observed sketch resize: {}x{} -> {}x{}", lastDrawWidth, lastDrawHeight, width, height);
        lastDrawWidth = width;
        lastDrawHeight = height;
    }

    public DesktopAppShell desktopAppShell() {
        return desktopAppShell;
    }

    @Override
    protected MonopolyEventBus eventBusOrNull() {
        return desktopAppShell.eventBusOrNull();
    }

    @Override
    public int mouseX() {
        return mouseX;
    }

    @Override
    public int mouseY() {
        return mouseY;
    }
}
