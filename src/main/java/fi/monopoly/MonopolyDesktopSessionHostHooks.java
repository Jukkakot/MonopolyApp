package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.components.Game;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;

/**
 * Adapts {@link MonopolyApp} runtime lifecycle behavior to the desktop session host coordinator.
 */
final class MonopolyDesktopSessionHostHooks implements DesktopSessionHostCoordinator.Hooks {
    private final MonopolyDesktopRuntimeBridge runtimeBridge;

    MonopolyDesktopSessionHostHooks(MonopolyDesktopRuntimeBridge runtimeBridge) {
        this.runtimeBridge = runtimeBridge;
    }

    @Override
    public void shutdownSessionRuntime() {
        runtimeBridge.shutdownSessionRuntime();
    }

    @Override
    public void disposeGame(Game game) {
        game.dispose();
    }

    @Override
    public void disposeControlLayer() {
        runtimeBridge.disposeControlLayer();
    }

    @Override
    public void initializeControlLayer() {
        runtimeBridge.initializeControlLayer();
    }

    @Override
    public void applyDefaultTextFont() {
        runtimeBridge.applyDefaultTextFont();
    }

    @Override
    public Game createGame(fi.monopoly.domain.session.SessionState restoredState) {
        return runtimeBridge.createGame(restoredState);
    }

    @Override
    public void flushPendingChanges() {
        runtimeBridge.flushPendingChanges();
    }
}
