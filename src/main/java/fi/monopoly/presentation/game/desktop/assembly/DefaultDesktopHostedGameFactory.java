package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.session.local.DesktopHostedGame;

/**
 * Default local desktop hosted-game factory backed by the current {@link Game} host.
 */
public final class DefaultDesktopHostedGameFactory implements DesktopHostedGameFactory {
    @Override
    public DesktopHostedGame create(
            MonopolyRuntime runtime,
            SessionState restoredState,
            LocalSessionActions localSessionActions
    ) {
        return new Game(runtime, restoredState, localSessionActions);
    }
}
