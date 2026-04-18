package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.components.Game;
import fi.monopoly.presentation.game.GameSidebarPresenter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static processing.event.Event.CTRL;
import static processing.event.KeyEvent.PRESS;

class MonopolyAppPersistenceTest {
    @TempDir
    Path tempDir;

    private static void dispatchCtrlKey(MonopolyRuntime runtime, char key) {
        runtime.eventBus().sendConsumableEvent(new KeyEvent(
                new Object(),
                System.currentTimeMillis(),
                PRESS,
                CTRL,
                key,
                key
        ));
        runtime.eventBus().flushPendingChanges();
    }

    private static GameSidebarPresenter.SidebarState currentSidebarState(Game game) {
        try {
            var method = Game.class.getDeclaredMethod("createSidebarState");
            method.setAccessible(true);
            return (GameSidebarPresenter.SidebarState) method.invoke(game);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to inspect sidebar state", e);
        }
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("monopoly.localSavePath");
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void ctrlSaveAndCtrlLoadRoundTripCurrentLocalSession() {
        Path snapshotPath = tempDir.resolve("local-session.json");
        System.setProperty("monopoly.localSavePath", snapshotPath.toString());

        MonopolyApp app = new MonopolyApp();
        app.width = MonopolyApp.DEFAULT_WINDOW_WIDTH;
        app.height = MonopolyApp.DEFAULT_WINDOW_HEIGHT;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        MonopolyApp.p5 = controlP5;
        MonopolyApp.font10 = font;
        MonopolyApp.font20 = font;
        MonopolyApp.font30 = font;

        MonopolyRuntime runtime = MonopolyRuntime.initialize(app, controlP5, font, font, font);
        Game game = new Game(runtime);
        app.setGameForTest(game);
        runtime.eventBus().flushPendingChanges();

        int originalCash = game.sessionStateForPersistence().players().get(0).cash();

        dispatchCtrlKey(runtime, 's');

        assertTrue(Files.exists(snapshotPath));
        assertTrue(currentSidebarState(game).persistenceNotice().startsWith("Saved "));

        runtime.gameSession().players().getPlayers().get(0).addMoney(-200);
        assertEquals(originalCash - 200, game.sessionStateForPersistence().players().get(0).cash());

        dispatchCtrlKey(MonopolyRuntime.get(), 'l');

        Game reloadedGame = app.currentGame();
        assertNotNull(reloadedGame);
        assertEquals(originalCash, reloadedGame.sessionStateForPersistence().players().get(0).cash());
        assertTrue(currentSidebarState(reloadedGame).persistenceNotice().startsWith("Loaded "));
    }
}
