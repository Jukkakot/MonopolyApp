package fi.monopoly.client.session.local;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.session.local.HostedLocalSession;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGameTestAccess;

/**
 * Client-session adapter backed by the current embedded desktop host.
 *
 * <p>This keeps the existing local desktop hosting flow intact while moving the app itself toward a
 * host/client boundary. The Processing client now depends on this client-session seam instead of
 * reaching directly into the desktop host coordinator for normal session operations.</p>
 */
public final class LocalDesktopClientSession implements ClientSession {
    private final HostedLocalSession hostedSession;

    public LocalDesktopClientSession(HostedLocalSession hostedSession) {
        this.hostedSession = hostedSession;
    }

    @Override
    public SessionState currentState() {
        return hostedSession.currentState();
    }

    @Override
    public void replaceState(SessionState restoredState) {
        hostedSession.replaceState(restoredState);
    }

    @Override
    public void startFreshSession() {
        hostedSession.startFreshSession();
    }

    @Override
    public void advanceFrame() {
        hostedSession.advanceHostFrame();
    }

    @Override
    public LocalSessionPersistenceResult saveLocalSession() {
        return hostedSession.saveLocalSession();
    }

    @Override
    public LocalSessionPersistenceResult loadLocalSession() {
        return hostedSession.loadLocalSession();
    }

    @Override
    public ClientSessionView currentView() {
        return hostedSession.currentView();
    }

    @Override
    public ClientSessionSnapshot snapshot() {
        return hostedSession.snapshot();
    }

    @Override
    public void addListener(ClientSessionListener listener) {
        hostedSession.addListener(listener);
    }

    @Override
    public void removeListener(ClientSessionListener listener) {
        hostedSession.removeListener(listener);
    }

    @Override
    public void showPersistenceNotice(String message) {
        hostedSession.showPersistenceNotice(message);
    }

    public DesktopHostedGameTestAccess testAccess() {
        return hostedSession.testAccess();
    }
}
