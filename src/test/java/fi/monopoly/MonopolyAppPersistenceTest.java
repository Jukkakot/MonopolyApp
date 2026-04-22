package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.DesktopRuntimeResources;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.components.Game;
import fi.monopoly.host.session.local.DesktopHostedGame;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.host.session.local.GameBackedDesktopHostedGame;
import fi.monopoly.presentation.game.desktop.ui.GameSidebarPresenter;
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
        return game.testFacade().sidebarState();
    }

    private static boolean paused(Game game) {
        return game.testFacade().sessionState().paused();
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
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);

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
        DesktopRuntimeResources.setControlLayer(controlP5);
        DesktopRuntimeResources.setFonts(font, font, font);

        MonopolyRuntime runtime = MonopolyRuntime.initialize(app, controlP5, font, font, font);
        Game game = new Game(runtime, null, new LocalSessionActions(app::saveLocalSession, app::loadLocalSession));
        DesktopHostedGameTestAccess testAccess = app.desktopAppShell().testAccess();
        testAccess.setHostedGame(new GameBackedDesktopHostedGame(game));
        runtime.eventBus().flushPendingChanges();

        int originalCash = game.sessionStateForPersistence().players().get(0).cash();

        dispatchCtrlKey(runtime, 's');

        assertTrue(Files.exists(snapshotPath));
        assertTrue(currentSidebarState(game).persistenceNotice().startsWith("Saved "));

        runtime.gameSession().players().getPlayers().get(0).addMoney(-200);
        assertEquals(originalCash - 200, game.sessionStateForPersistence().players().get(0).cash());

        dispatchCtrlKey(app.desktopAppShell().runtime(), 'l');

        DesktopHostedGame reloadedHostedGame = testAccess.currentHostedGame();
        Game reloadedGame = testAccess.currentConcreteGameOrNull();
        assertNotNull(reloadedGame);
        assertEquals(originalCash, reloadedGame.sessionStateForPersistence().players().get(0).cash());
        assertTrue(paused(reloadedGame));
        assertTrue(currentSidebarState(reloadedGame).persistenceNotice().startsWith("Loaded "));
    }

    @Test
    void savePopupCanBeClosedDuringComputerTurn() {
        Path snapshotPath = tempDir.resolve("local-session.json");
        System.setProperty("monopoly.localSavePath", snapshotPath.toString());
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);

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
        DesktopRuntimeResources.setControlLayer(controlP5);
        DesktopRuntimeResources.setFonts(font, font, font);

        MonopolyRuntime runtime = MonopolyRuntime.initialize(app, controlP5, font, font, font);
        Game game = new Game(runtime, null, new LocalSessionActions(app::saveLocalSession, app::loadLocalSession));
        app.desktopAppShell().testAccess().setHostedGame(new GameBackedDesktopHostedGame(game));
        runtime.eventBus().flushPendingChanges();

        assertTrue(runtime.gameSession().players().getTurn().isComputerControlled());

        app.saveLocalSession();

        assertNotNull(runtime.popupService().activePopupMessage());
        assertTrue(runtime.popupService().activePopupMessage().startsWith("Saved "));
        assertTrue(runtime.popupService().triggerPrimaryAction());
        assertFalse(runtime.popupService().isAnyVisible());
    }

    @Test
    void loadRebuildClearsOldPopupStateAndStartsLoadedGamePaused() {
        Path snapshotPath = tempDir.resolve("local-session.json");
        System.setProperty("monopoly.localSavePath", snapshotPath.toString());
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);

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
        DesktopRuntimeResources.setControlLayer(controlP5);
        DesktopRuntimeResources.setFonts(font, font, font);

        MonopolyRuntime runtime = MonopolyRuntime.initialize(app, controlP5, font, font, font);
        Game game = new Game(runtime, null, new LocalSessionActions(app::saveLocalSession, app::loadLocalSession));
        DesktopHostedGameTestAccess testAccess = app.desktopAppShell().testAccess();
        testAccess.setHostedGame(new GameBackedDesktopHostedGame(game));
        runtime.eventBus().flushPendingChanges();

        app.saveLocalSession();
        assertTrue(Files.exists(snapshotPath));
        runtime.popupService().hideAll();

        runtime.popupService().show("Stale popup before load");
        runtime.popupService().show("Pending popup before load");
        assertEquals("Stale popup before load", runtime.popupService().activePopupMessage());

        app.loadLocalSession();

        DesktopHostedGame reloadedHostedGame = testAccess.currentHostedGame();
        Game reloadedGame = testAccess.currentConcreteGameOrNull();
        assertNotNull(reloadedGame);
        assertNotSame(game, reloadedGame);
        assertTrue(paused(reloadedGame));
        MonopolyRuntime reloadedRuntime = app.desktopAppShell().runtime();
        assertTrue(reloadedRuntime.eventBus().eventListenerCount() > 0);
        assertNotNull(reloadedRuntime.popupService().activePopupMessage());
        assertTrue(reloadedRuntime.popupService().activePopupMessage().startsWith("Loaded "));
    }
}
