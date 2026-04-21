package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGame;
import fi.monopoly.presentation.game.desktop.session.LocalSessionActions;

/**
 * Factory seam for constructing the active desktop-hosted game instance.
 *
 * <p>This keeps concrete hosted-game creation out of the client-desktop bootstrap bridge so the
 * Processing-side shell no longer needs to know which host implementation backs the local session.
 */
public interface DesktopHostedGameFactory {
    DesktopHostedGame create(
            MonopolyRuntime runtime,
            SessionState restoredState,
            LocalSessionActions localSessionActions
    );
}
