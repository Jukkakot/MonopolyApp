package fi.monopoly.host.session.local;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;

/**
 * Owns the active desktop {@link Game} instance and rebuilds it when local session state changes.
 *
 * <p>This coordinator isolates desktop session hosting concerns from {@code MonopolyApp}: loading a
 * snapshot, preparing restored state for UI-safe startup, tearing down the old runtime shell, and
 * creating the next local game instance. The resulting seam is close to what a future backend-fed
 * desktop client will still need, even after authoritative session ownership moves elsewhere.</p>
 */
public final class DesktopSessionHostCoordinator implements SessionHost {
    private DesktopHostedGame game;
    private final Hooks hooks;
    private final DesktopHostedGameTestAccess testAccess = new DesktopHostedGameTestAccess(new HostedGameAccess());

    public DesktopSessionHostCoordinator(Hooks hooks) {
        this.hooks = hooks;
    }

    @Override
    public SessionState currentState() {
        return game != null ? game.sessionStateForPersistence() : null;
    }

    @Override
    public void replaceState(SessionState restoredState) {
        rebuildGame(restoredState);
    }

    public void rebuildFreshGame() {
        rebuildGame(null);
    }

    public DesktopHostedGame currentGame() {
        return game;
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }

    public void showPersistenceNotice(String message) {
        if (game != null) {
            game.showPersistenceNotice(message);
        }
    }

    private void rebuildGame(SessionState restoredState) {
        SessionState preparedRestoredState = prepareRestoredStateForRebuild(restoredState);
        hooks.shutdownSessionRuntime();
        if (game != null) {
            hooks.disposeGame(game);
        }
        hooks.shutdownSessionRuntime();
        hooks.disposeControlLayer();
        hooks.initializeControlLayer();
        hooks.applyDefaultTextFont();
        game = hooks.createGame(preparedRestoredState);
        hooks.flushPendingChanges();
    }

    private SessionState prepareRestoredStateForRebuild(SessionState restoredState) {
        if (restoredState == null || restoredState.status() != SessionStatus.IN_PROGRESS) {
            return restoredState;
        }
        return new SessionState(
                restoredState.sessionId(),
                restoredState.version(),
                SessionStatus.PAUSED,
                restoredState.seats(),
                restoredState.players(),
                restoredState.properties(),
                restoredState.turn(),
                restoredState.pendingDecision(),
                restoredState.auctionState(),
                restoredState.activeDebt(),
                restoredState.tradeState(),
                restoredState.turnContinuationState(),
                restoredState.winnerPlayerId()
        );
    }

    public interface Hooks {
        void shutdownSessionRuntime();

        void disposeGame(DesktopHostedGame game);

        void disposeControlLayer();

        void initializeControlLayer();

        void applyDefaultTextFont();

        DesktopHostedGame createGame(SessionState restoredState);

        void flushPendingChanges();
    }

    private final class HostedGameAccess implements DesktopHostedGameTestAccess.HostedGameAccess {
        @Override
        public DesktopHostedGame currentHostedGame() {
            return game;
        }

        @Override
        public void setHostedGame(DesktopHostedGame game) {
            DesktopSessionHostCoordinator.this.game = game;
        }
    }
}
