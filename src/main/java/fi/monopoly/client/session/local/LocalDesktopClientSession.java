package fi.monopoly.client.session.local;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceUseCase;
import fi.monopoly.application.session.persistence.SessionPersistenceService;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGame;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-session adapter backed by the current embedded desktop host.
 *
 * <p>This keeps the existing local desktop hosting flow intact while moving the app itself toward a
 * host/client boundary. The Processing client now depends on this client-session seam instead of
 * reaching directly into the desktop host coordinator for normal session operations.</p>
 */
public final class LocalDesktopClientSession implements ClientSession {
    private final DesktopSessionHostCoordinator desktopSessionHostCoordinator;
    private final LocalSessionPersistenceUseCase persistenceUseCase;
    private final Set<ClientSessionListener> listeners = new LinkedHashSet<>();

    public LocalDesktopClientSession(DesktopSessionHostCoordinator desktopSessionHostCoordinator) {
        this(
                desktopSessionHostCoordinator,
                new LocalSessionPersistenceUseCase(
                        new SessionPersistenceService(),
                        desktopSessionHostCoordinator
                )
        );
    }

    public LocalDesktopClientSession(
            DesktopSessionHostCoordinator desktopSessionHostCoordinator,
            LocalSessionPersistenceUseCase persistenceUseCase
    ) {
        this.desktopSessionHostCoordinator = desktopSessionHostCoordinator;
        this.persistenceUseCase = persistenceUseCase;
    }

    @Override
    public SessionState currentState() {
        return desktopSessionHostCoordinator.currentState();
    }

    @Override
    public void replaceState(SessionState restoredState) {
        desktopSessionHostCoordinator.replaceState(restoredState);
        publishSnapshot();
    }

    @Override
    public void startFreshSession() {
        desktopSessionHostCoordinator.rebuildFreshGame();
        publishSnapshot();
    }

    @Override
    public void advanceFrame() {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        if (game != null) {
            game.advanceFrame();
        }
    }

    @Override
    public LocalSessionPersistenceResult saveLocalSession() {
        return persistenceUseCase.saveLocalSession();
    }

    @Override
    public LocalSessionPersistenceResult loadLocalSession() {
        LocalSessionPersistenceResult result = persistenceUseCase.loadLocalSession();
        publishSnapshot();
        return result;
    }

    @Override
    public ClientSessionView currentView() {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        return game != null ? new GameClientSessionView(game) : null;
    }

    @Override
    public ClientSessionSnapshot snapshot() {
        return ClientSessionSnapshot.from(currentState(), currentView() != null);
    }

    @Override
    public void addListener(ClientSessionListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        listener.onSnapshotChanged(snapshot());
    }

    @Override
    public void removeListener(ClientSessionListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void showPersistenceNotice(String message) {
        desktopSessionHostCoordinator.showPersistenceNotice(message);
        publishSnapshot();
    }

    public DesktopHostedGame currentGameForTest() {
        return desktopSessionHostCoordinator.currentGame();
    }

    public void setGameForTest(DesktopHostedGame game) {
        desktopSessionHostCoordinator.setGameForTest(game);
        publishSnapshot();
    }

    private void publishSnapshot() {
        ClientSessionSnapshot snapshot = snapshot();
        for (ClientSessionListener listener : List.copyOf(listeners)) {
            listener.onSnapshotChanged(snapshot);
        }
    }

    private record GameClientSessionView(DesktopHostedGame game) implements ClientSessionView {
        @Override
        public void draw() {
            game.draw();
        }

        @Override
        public List<String> debugPerformanceLines(float fps) {
            return game.debugPerformanceLines(fps);
        }
    }
}
