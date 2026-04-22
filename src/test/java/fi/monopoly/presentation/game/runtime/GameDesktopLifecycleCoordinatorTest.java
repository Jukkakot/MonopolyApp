package fi.monopoly.presentation.game.runtime;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.presentation.game.desktop.runtime.GameDesktopLifecycleCoordinator;
import fi.monopoly.support.TestDesktopRuntimeFactory;
import org.junit.jupiter.api.Test;

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
        return TestDesktopRuntimeFactory.create(1200, 800).runtime();
    }
}
