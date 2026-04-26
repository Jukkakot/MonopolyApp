package fi.monopoly.host.session.local;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceUseCase;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.application.session.persistence.SessionPersistenceService;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.DesktopSessionRenderView;

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
public final class EmbeddedDesktopSessionHost implements HostedLocalSession, SessionCommandPort {
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
        registerExternalCommandDelegate();
        publishSnapshot();
    }

    @Override
    public void startFreshSession() {
        desktopSessionHostCoordinator.rebuildFreshGame();
        registerExternalCommandDelegate();
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
    public DesktopSessionRenderView currentView() {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        return game != null ? new EmbeddedDesktopRenderView(game.view()) : null;
    }

    public ClientSessionSnapshot currentSnapshot() {
        return ClientSessionSnapshot.from(currentState(), currentView() != null);
    }

    @Override
    public CommandResult handle(SessionCommand command) {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        if (game == null) {
            throw new IllegalStateException("Cannot submit command: no active hosted game");
        }
        CommandResult result = game.submitCommand(command);
        if (result.accepted()) {
            publishSnapshot();
        }
        return result;
    }

    @Override
    public void addListener(ClientSessionListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
        listener.onSnapshotChanged(currentSnapshot());
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

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }

    private void registerExternalCommandDelegate() {
        DesktopHostedGame game = desktopSessionHostCoordinator.currentGame();
        if (game != null) {
            game.setExternalCommandDelegate(this);
        }
    }

    private void publishSnapshot() {
        ClientSessionSnapshot snapshot = currentSnapshot();
        for (ClientSessionListener listener : List.copyOf(listeners)) {
            listener.onSnapshotChanged(snapshot);
        }
    }

    private record EmbeddedDesktopRenderView(DesktopHostedGameView gameView) implements DesktopSessionRenderView {
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
