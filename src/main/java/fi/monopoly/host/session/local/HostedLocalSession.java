package fi.monopoly.host.session.local;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;

/**
 * Host-owned local session seam used by the desktop client adapter.
 *
 * <p>This keeps session lifecycle, persistence, and snapshot publication on the host side even
 * while the current implementation still runs in the same process as the client.</p>
 */
public interface HostedLocalSession extends SessionHost {
    void startFreshSession();

    void advanceHostFrame();

    LocalSessionPersistenceResult saveLocalSession();

    LocalSessionPersistenceResult loadLocalSession();

    ClientSessionView currentView();

    ClientSessionSnapshot snapshot();

    void addListener(ClientSessionListener listener);

    void removeListener(ClientSessionListener listener);

    void showPersistenceNotice(String message);

    DesktopHostedGameTestAccess testAccess();
}
