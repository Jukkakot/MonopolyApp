package fi.monopoly.host.session.local;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.DesktopLocalSessionControls;
import fi.monopoly.client.session.desktop.DesktopSessionViewPort;

/**
 * Host-owned local session seam used by the desktop client adapter.
 *
 * <p>This keeps session lifecycle, persistence, and snapshot publication on the host side even
 * while the current implementation still runs in the same process as the client.</p>
 */
public interface HostedLocalSession extends ClientSessionUpdates, SessionHost, SessionCommandPort, DesktopLocalSessionControls, DesktopSessionViewPort {
    void advanceHostFrame();
}
