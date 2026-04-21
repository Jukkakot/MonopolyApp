package fi.monopoly.presentation.game.desktop.runtime;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.event.MonopolyEventListener;

import java.util.List;

/**
 * Owns desktop gameplay shell shutdown so resource cleanup no longer lives directly in
 * {@code Game}.
 */
public final class GameDesktopLifecycleCoordinator {

    public void dispose(
            MonopolyRuntime runtime,
            MonopolyEventListener eventListener,
            Players players,
            Dices dices,
            List<MonopolyButton> buttons
    ) {
        runtime.eventBus().removeListener(eventListener);
        runtime.popupService().hideAll();
        if (players != null) {
            players.dispose();
        }
        if (dices != null) {
            dices.dispose();
        }
        for (MonopolyButton button : buttons) {
            button.dispose();
        }
    }
}
