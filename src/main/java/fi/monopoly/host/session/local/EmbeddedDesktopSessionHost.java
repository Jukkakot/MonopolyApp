package fi.monopoly.host.session.local;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceUseCase;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.application.session.persistence.SessionPersistenceService;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGameTestAccess;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGame;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGameView;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Embedded local session host for the current single-process desktop client.
 *
 * <p>This is the first explicit host object in the codebase. It still runs in the same process as
 * the Processing client, but it groups local session ownership behind one host abstraction instead
 * of letting {@code MonopolyApp} coordinate host concerns directly. That makes the later jump to a
 * real remote host much narrower.</p>
 */
public final class EmbeddedDesktopSessionHost implements HostedLocalSession {
    private final DesktopSessionHostCoordinator desktopSessionHostCoordinator;
    private final LocalSessionPersistenceUseCase persistenceUseCase;
    private final Set<ClientSessionListener> listeners = new LinkedHashSet<>();
    private final DesktopHostedGameTestAccess testAccess;

    public EmbeddedDesktopSessionHost(DesktopSessionHostCoordinator.Hooks hooks) {
        this.desktopSessionHostCoordinator = new DesktopSessionHostCoordinator(hooks);
        this.persistenceUseCase = new LocalSessionPersistenceUseCase(
                new SessionPersistenceService(),
                desktopSessionHostCoordinator
        );
        this.testAccess = new DesktopHostedGameTestAccess(new TestHostedGameAccess());
    }

    @Override
    public fi.monopoly.domain.session.SessionState currentState() {
        return desktopSessionHostCoordinator.currentState();
    }

    @Override
    public void replaceState(fi.monopoly.domain.session.SessionState restoredState) {
        desktopSessionHostCoordinator.replaceState(restoredState);
        publishSnapshot();
    }

    @Override
    public void startFreshSession() {
        desktopSessionHostCoordinator.rebuildFreshGame();
        publishSnapshot();
    }

    @Override
    public void advanceHostFrame() {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        if (game != null) {
            game.advanceHostedFrame();
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
        return game != null ? new GameClientSessionView(game.view()) : null;
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

    @Override
    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }

    private void publishSnapshot() {
        ClientSessionSnapshot snapshot = snapshot();
        for (ClientSessionListener listener : List.copyOf(listeners)) {
            listener.onSnapshotChanged(snapshot);
        }
    }

    private record GameClientSessionView(DesktopHostedGameView gameView) implements ClientSessionView {
        @Override
        public void draw() {
            gameView.draw();
        }

        @Override
        public List<String> debugPerformanceLines(float fps) {
            return gameView.debugPerformanceLines(fps);
        }
    }

    private final class TestHostedGameAccess implements DesktopHostedGameTestAccess.HostedGameAccess {
        @Override
        public DesktopHostedGame currentHostedGame() {
            return desktopSessionHostCoordinator.currentGame();
        }

        @Override
        public void setHostedGame(DesktopHostedGame game) {
            desktopSessionHostCoordinator.testAccess().setHostedGame(game);
            publishSnapshot();
        }
    }
}
