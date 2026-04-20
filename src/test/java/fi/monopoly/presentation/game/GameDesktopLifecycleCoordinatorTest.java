package fi.monopoly.presentation.game;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.presentation.game.desktop.runtime.GameDesktopLifecycleCoordinator;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GameDesktopLifecycleCoordinatorTest {

    @Test
    void disposeHidesPopupsAndDisposesPlayersDiceAndButtons() {
        MonopolyRuntime runtime = initHeadlessRuntime();
        Players players = new Players(runtime);
        Dices dices = new Dices(runtime);
        MonopolyButton buttonA = new MonopolyButton(runtime, "a");
        MonopolyButton buttonB = new MonopolyButton(runtime, "b");
        buttonA.show();
        buttonB.show();
        dices.show();
        runtime.popupService().show("Popup");

        new GameDesktopLifecycleCoordinator().dispose(
                runtime,
                event -> false,
                players,
                dices,
                List.of(buttonA, buttonB)
        );

        assertFalse(runtime.popupService().isAnyVisible());
        assertFalse(buttonA.isVisible());
        assertFalse(buttonB.isVisible());
        assertFalse(dices.isVisible());
        assertFalse(players.getPlayers().iterator().hasNext());
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = 1200;
        app.height = 800;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }
}
