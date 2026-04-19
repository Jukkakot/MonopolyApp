package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.application.session.SessionHost;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceCoordinator;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceUiHooks;
import fi.monopoly.application.session.persistence.SessionPersistenceService;
import fi.monopoly.components.Game;
import fi.monopoly.components.PlayerToken;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.event.MonopolyEventObserver;
import fi.monopoly.presentation.game.DesktopSessionHostCoordinator;
import fi.monopoly.presentation.game.LocalSessionActions;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.UiTokens;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import processing.awt.PSurfaceAWT;
import processing.core.PFont;
import processing.core.PImage;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class MonopolyApp extends MonopolyEventObserver {
    public static final int TARGET_FRAME_RATE = 120;
    public static final char ENTER = '\n';
    public static final char SPACE = ' ';
    public static final int DEFAULT_WINDOW_WIDTH = 1700;
    public static final int DEFAULT_WINDOW_HEIGHT = 996;
    public static MonopolyApp self;
    public static boolean DEBUG_MODE = false;
    public static boolean SKIP_ANNIMATIONS = false;
    public static ControlP5 p5;
    public static PFont font10, font20, font30;
    private static Map<String, PImage> IMAGES = new HashMap<>();
    private static final Map<String, PImage> TINTED_IMAGE_CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PImage> eldest) {
            return size() > 128;
        }
    };
    private static long coloredImageCopies;
    private final SessionPersistenceService sessionPersistenceService = new SessionPersistenceService();
    private final DesktopSessionHostCoordinator desktopSessionHostCoordinator = new DesktopSessionHostCoordinator(
            new DesktopSessionHostCoordinator.Hooks() {
                @Override
                public void shutdownSessionRuntime() {
                    MonopolyApp.this.shutdownCurrentSessionRuntime();
                }

                @Override
                public void disposeGame(Game game) {
                    game.dispose();
                }

                @Override
                public void disposeControlLayer() {
                    if (p5 != null) {
                        p5.dispose();
                    }
                }

                @Override
                public void initializeControlLayer() {
                    p5 = new ControlP5(MonopolyApp.this);
                    MonopolyRuntime.initialize(MonopolyApp.this, p5, font10, font20, font30);
                }

                @Override
                public void applyDefaultTextFont() {
                    MonopolyApp.this.applyDefaultTextFont();
                }

                @Override
                public Game createGame(fi.monopoly.domain.session.SessionState restoredState) {
                    LocalSessionActions localSessionActions = new LocalSessionActions(
                            MonopolyApp.this::saveLocalSession,
                            MonopolyApp.this::loadLocalSession
                    );
                    return new Game(MonopolyRuntime.get(), restoredState, localSessionActions);
                }

                @Override
                public void flushPendingChanges() {
                    MonopolyRuntime.get().eventBus().flushPendingChanges();
                }
            }
    );
    private final SessionHost sessionHost = desktopSessionHostCoordinator;
    private final LocalSessionPersistenceUiHooks localSessionPersistenceUiHooks = new LocalSessionPersistenceUiHooks() {
        @Override
        public void showPopup(String message) {
            MonopolyRuntime.get().popupService().showManualDecision(
                    message,
                    new ButtonProps(fi.monopoly.text.UiTexts.text("popup.ok.label"), null)
            );
        }

        @Override
        public void showPersistenceNotice(String message) {
            desktopSessionHostCoordinator.showPersistenceNotice(message);
        }
    };
    private final LocalSessionPersistenceCoordinator localSessionPersistenceCoordinator =
            new LocalSessionPersistenceCoordinator(sessionPersistenceService, sessionHost, localSessionPersistenceUiHooks);
    private int lastDrawWidth = -1;
    private int lastDrawHeight = -1;

    public MonopolyApp() {
        self = this;
        MonopolyRuntime.initialize(this, null, null, null, null);
    }

    /**
     * @param name  name of the image
     * @param color color the image should be tinted
     * @return Instance of an image. If color is given, then it returns a copy of the image tinted with the given color
     */
    public static PImage getImage(String name, Color color) {
        PImage image = IMAGES.get(name);
        if (image == null) {
            return null;
        }
        if (color != null) {
            int tintColor = MonopolyUtils.toColor(self, color);
            String cacheKey = name + "#" + tintColor;
            PImage cachedImage = TINTED_IMAGE_CACHE.get(cacheKey);
            if (cachedImage != null) {
                return cachedImage;
            }
            PImage tintedImage = getColoredCopy(image, tintColor);
            TINTED_IMAGE_CACHE.put(cacheKey, tintedImage);
            return tintedImage;
        }
        return image;
    }

    private static PImage getColoredCopy(PImage img, int color) {
        coloredImageCopies++;
        PImage result = self.createImage(img.width, img.height, RGB);
        for (int i = 0; i < img.pixels.length; i++) {
            int pixel = blendColor(img.pixels[i], color, DARKEST);
            result.pixels[i] = pixel;
        }
        result.mask(img);
        result.updatePixels();
        return result;
    }

    public static PImage getImage(String name) {
        return getImage(name, null);
    }

    public static PImage getImage(SpotType spotType) {
        PImage image = getImage(spotType.streetType.imgName, null);
        if (image == null) {
            String imgName = spotType.streetType.imgName;
            if (imgName != null) {
                image = getImage(imgName.substring(0, imgName.indexOf(".")) + spotType.id + imgName.substring(imgName.indexOf(".")), null);
            }
        }
        return image;
    }

    public static long getColoredImageCopies() {
        return coloredImageCopies;
    }

    public void settings() {
        size(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT, FX2D);
    }

    public void setup() {
        frameRate(TARGET_FRAME_RATE);
        initImages();
        configureWindowSizing();
        font10 = createFont("Monopoly Regular.ttf", 10);
        font20 = createFont("Monopoly Regular.ttf", 20);
        font30 = createFont("Monopoly Regular.ttf", 30);
        desktopSessionHostCoordinator.rebuildFreshGame();
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
        desktopSessionHostCoordinator.currentGame().draw();
        if (DEBUG_MODE) {
            push();
            fill(22, 36, 31, 190);
            noStroke();
            rect(12, 12, 270, 144, 14);
            fill(245, 245, 245);
            textAlign(LEFT, TOP);
            textFont(font20);
            float debugTextY = 22;
            for (String line : desktopSessionHostCoordinator.currentGame().debugPerformanceLines(frameRate)) {
                text(line, 24, debugTextY);
                debugTextY += 20;
            }
            fill(255, 105, 180);
            noStroke();
            circle(mouseX, mouseY, 20);
            textFont(font20);
            text(fi.monopoly.text.UiTexts.text("app.debug.mouseCoords", mouseX, mouseY), mouseX - 40, mouseY + 30);
            pop();
        }
    }

    public void saveLocalSession() {
        localSessionPersistenceCoordinator.saveLocalSession();
    }

    public void loadLocalSession() {
        localSessionPersistenceCoordinator.loadLocalSession();
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

    private void initImages() {
        coloredImageCopies = 0;
        TINTED_IMAGE_CACHE.clear();
        String dirPath = "src/main/resources/img/";
        List<String> fileNames = listFiles(dirPath);
        for (String fileName : fileNames) {
            IMAGES.put(fileName, loadImage(dirPath + fileName));
        }
        IMAGES.get("BigToken.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        IMAGES.get("BigTokenHover.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        IMAGES.get("BigTokenPressed.png").resize(PlayerToken.PLAYER_TOKEN_BIG_DIAMETER, PlayerToken.PLAYER_TOKEN_BIG_DIAMETER);
        println("Finished loading", IMAGES.size(), "images.");
    }

    private List<String> listFiles(String dir) {
        return Stream.of(Objects.requireNonNull(new File(dir).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .toList();
    }

    Game currentGame() {
        return desktopSessionHostCoordinator.currentGame();
    }

    void setGameForTest(Game game) {
        desktopSessionHostCoordinator.setGameForTest(game);
    }

    private void shutdownCurrentSessionRuntime() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime == null) {
            return;
        }
        runtime.popupService().hideAll();
        runtime.setGameSession(null);
        runtime.eventBus().flushPendingChanges();
        runtime.popupService().hideAll();
    }

    private void applyDefaultTextFont() {
        if (font10 == null) {
            return;
        }
        try {
            textFont(font10);
        } catch (RuntimeException e) {
            log.debug("Skipping textFont apply during runtime rebuild because graphics context is not ready yet");
        }
    }
}
