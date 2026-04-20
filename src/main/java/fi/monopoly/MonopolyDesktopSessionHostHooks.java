package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.components.Game;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;
import fi.monopoly.presentation.game.desktop.session.LocalSessionActions;

/**
 * Adapts {@link MonopolyApp} runtime lifecycle behavior to the desktop session host coordinator.
 */
final class MonopolyDesktopSessionHostHooks implements DesktopSessionHostCoordinator.Hooks {
    private final MonopolyApp app;

    MonopolyDesktopSessionHostHooks(MonopolyApp app) {
        this.app = app;
    }

    @Override
    public void shutdownSessionRuntime() {
        app.shutdownCurrentSessionRuntimeRef();
    }

    @Override
    public void disposeGame(Game game) {
        game.dispose();
    }

    @Override
    public void disposeControlLayer() {
        if (MonopolyApp.p5 != null) {
            MonopolyApp.p5.dispose();
        }
    }

    @Override
    public void initializeControlLayer() {
        MonopolyApp.p5 = new ControlP5(app);
        MonopolyRuntime.initialize(app, MonopolyApp.p5, MonopolyApp.font10, MonopolyApp.font20, MonopolyApp.font30);
    }

    @Override
    public void applyDefaultTextFont() {
        app.applyDefaultTextFontRef();
    }

    @Override
    public Game createGame(fi.monopoly.domain.session.SessionState restoredState) {
        LocalSessionActions localSessionActions = new LocalSessionActions(
                app::saveLocalSession,
                app::loadLocalSession
        );
        return new Game(MonopolyRuntime.get(), restoredState, localSessionActions);
    }

    @Override
    public void flushPendingChanges() {
        MonopolyRuntime.get().eventBus().flushPendingChanges();
    }
}
