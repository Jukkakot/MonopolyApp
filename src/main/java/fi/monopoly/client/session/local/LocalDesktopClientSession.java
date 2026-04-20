package fi.monopoly.client.session.local;

import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;

import java.util.List;

/**
 * Client-session adapter backed by the current embedded desktop host.
 *
 * <p>This keeps the existing local desktop hosting flow intact while moving the app itself toward a
 * host/client boundary. The Processing client now depends on this client-session seam instead of
 * reaching directly into the desktop host coordinator for normal session operations.</p>
 */
public final class LocalDesktopClientSession implements ClientSession {
    private final DesktopSessionHostCoordinator desktopSessionHostCoordinator;

    public LocalDesktopClientSession(DesktopSessionHostCoordinator desktopSessionHostCoordinator) {
        this.desktopSessionHostCoordinator = desktopSessionHostCoordinator;
    }

    @Override
    public SessionState currentState() {
        return desktopSessionHostCoordinator.currentState();
    }

    @Override
    public void replaceState(SessionState restoredState) {
        desktopSessionHostCoordinator.replaceState(restoredState);
    }

    @Override
    public void startFreshSession() {
        desktopSessionHostCoordinator.rebuildFreshGame();
    }

    @Override
    public ClientSessionView currentView() {
        Game game = desktopSessionHostCoordinator.currentGame();
        return game != null ? new GameClientSessionView(game) : null;
    }

    @Override
    public void showPersistenceNotice(String message) {
        desktopSessionHostCoordinator.showPersistenceNotice(message);
    }

    public Game currentGameForTest() {
        return desktopSessionHostCoordinator.currentGame();
    }

    public void setGameForTest(Game game) {
        desktopSessionHostCoordinator.setGameForTest(game);
    }

    private record GameClientSessionView(Game game) implements ClientSessionView {
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
