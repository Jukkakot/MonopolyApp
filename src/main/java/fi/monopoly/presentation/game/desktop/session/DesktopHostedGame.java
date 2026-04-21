package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.domain.session.SessionState;

/**
 * Minimal desktop-hosted gameplay surface needed by local session hosting and client-session
 * adapters.
 *
 * <p>This keeps embedded desktop session flow from depending on the full {@code Game} host type
 * when only session/view lifecycle operations are required.</p>
 */
public interface DesktopHostedGame {
    SessionState sessionStateForPersistence();

    void showPersistenceNotice(String notice);

    void advanceHostedFrame();

    DesktopHostedGameView view();

    void dispose();
}
