package fi.monopoly.host.session.local;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.desktop.DesktopLocalSessionControls;

/**
 * Host-owned local session seam used by the desktop client adapter.
 *
 * <p>This keeps session lifecycle, persistence, and snapshot publication on the host side even
 * while the current implementation still runs in the same process as the client.</p>
 */
public interface HostedLocalSession extends ClientSession, SessionHost, DesktopLocalSessionControls {
    void advanceHostFrame();

    DesktopHostedGameTestAccess testAccess();
}
