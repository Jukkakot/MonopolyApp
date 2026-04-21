package fi.monopoly.host.session.local;

import fi.monopoly.components.Game;

/**
 * Explicit test-only access seam for the currently hosted desktop game instance.
 *
 * <p>This keeps fixture injection and host inspection out of the normal desktop session API while
 * still allowing integration tests to replace or inspect the local hosted game when needed.</p>
 */
public final class DesktopHostedGameTestAccess {
    private final HostedGameAccess delegate;

    public DesktopHostedGameTestAccess(HostedGameAccess delegate) {
        this.delegate = delegate;
    }

    public DesktopHostedGame currentHostedGame() {
        return delegate.currentHostedGame();
    }

    public Game currentConcreteGameOrNull() {
        DesktopHostedGame hostedGame = currentHostedGame();
        return hostedGame instanceof Game concreteGame ? concreteGame : null;
    }

    public void setHostedGame(DesktopHostedGame game) {
        delegate.setHostedGame(game);
    }

    public interface HostedGameAccess {
        DesktopHostedGame currentHostedGame();

        void setHostedGame(DesktopHostedGame game);
    }
}
