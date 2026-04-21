package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.TextWrapUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.List;

class GamePerformanceBenchmarkTest {

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
        JailSpot.jailTimeLeftMap.clear();
    }

    @Test
    void printsBaselinePerformanceSnapshot() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        Player turnPlayer = game.players().getTurn();
        turnPlayer.addOwnedProperty(PropertyFactory.getProperty(SpotType.DB1));
        turnPlayer.addOwnedProperty(PropertyFactory.getProperty(SpotType.B2));

        String historyText = "Eka: Saavuit ruutuun BOARDWALK. Haluatko ostaa sen hintaan M400? "
                + "Tama on tarkoituksella pitka mittausteksti recent messages -rivityksen benchmarkiin.";

        long wrapNs = benchmarkWrap(runtime.app(), historyText, 3_000);
        long gameViewNs = benchmarkGameView(game, turnPlayer, 2_000);
        long tintNs = benchmarkTintedImageLookup(2_000);
        System.out.println("Game performance baseline:");
        System.out.println(" - wrapText avg: " + formatMillis(wrapNs / 3_000.0));
        System.out.println(" - createGameView avg: " + formatMillis(gameViewNs / 2_000.0));
        System.out.println(" - tinted image lookup avg: " + formatMillis(tintNs / 2_000.0));
        System.out.println(" - tinted copies created: " + MonopolyApp.getColoredImageCopies());
        System.out.println(" - debug overlay sample: " + String.join(" | ", game.debugPerformanceLines(runtime.app().frameRate)));
    }

    private static long benchmarkWrap(MonopolyApp app, String text, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            List.copyOf(TextWrapUtils.wrapText(app.g, text, 260));
        }
        return System.nanoTime() - start;
    }

    private static long benchmarkGameView(Game game, Player turnPlayer, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            game.createGameView(turnPlayer);
        }
        return System.nanoTime() - start;
    }

    private static long benchmarkTintedImageLookup(int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            MonopolyApp.getImage("BigToken.png", javafx.scene.paint.Color.PINK);
        }
        return System.nanoTime() - start;
    }

    private static String formatMillis(double nanos) {
        return String.format(java.util.Locale.ROOT, "%.2fms", nanos / 1_000_000.0);
    }

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        graphics.beginDraw();
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        graphics.textFont(font);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    private static void resetNextPlayerId() throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("NEXT_ID");
        field.setAccessible(true);
        field.setInt(null, 0);
    }
}
