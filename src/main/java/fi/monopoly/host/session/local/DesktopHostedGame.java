package fi.monopoly.host.session.local;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
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

    CommandResult submitCommand(SessionCommand command);

    void showPersistenceNotice(String notice);

    void advanceHostedFrame();

    DesktopHostedGameView view();

    void dispose();

    void setExternalCommandDelegate(SessionCommandPort delegate);
}
