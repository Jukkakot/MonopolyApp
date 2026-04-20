package fi.monopoly;

import controlP5.ControlP5;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.LocalSessionActions;
import lombok.extern.slf4j.Slf4j;

/**
 * Desktop runtime/bootstrap bridge around the current Processing host.
 *
 * <p>This isolates runtime shutdown, control-layer recreation, font application, and local game
 * creation from {@link MonopolyApp}. The app should remain a thin Processing shell while desktop
 * host bootstrap details live here as a dedicated adapter.</p>
 */
@Slf4j
final class MonopolyDesktopRuntimeBridge {
    private final MonopolyApp app;
    private final Runnable saveLocalSessionAction;
    private final Runnable loadLocalSessionAction;

    MonopolyDesktopRuntimeBridge(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction
    ) {
        this.app = app;
        this.saveLocalSessionAction = saveLocalSessionAction;
        this.loadLocalSessionAction = loadLocalSessionAction;
    }

    void shutdownSessionRuntime() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime == null) {
            return;
        }
        runtime.popupService().hideAll();
        runtime.setGameSession(null);
        runtime.eventBus().flushPendingChanges();
        runtime.popupService().hideAll();
    }

    void disposeControlLayer() {
        if (MonopolyApp.p5 != null) {
            MonopolyApp.p5.dispose();
        }
    }

    void initializeControlLayer() {
        MonopolyApp.p5 = new ControlP5(app);
        MonopolyRuntime.initialize(app, MonopolyApp.p5, MonopolyApp.font10, MonopolyApp.font20, MonopolyApp.font30);
    }

    void applyDefaultTextFont() {
        if (MonopolyApp.font10 == null) {
            return;
        }
        try {
            app.textFont(MonopolyApp.font10);
        } catch (RuntimeException e) {
            log.debug("Skipping textFont apply during runtime rebuild because graphics context is not ready yet");
        }
    }

    Game createGame(SessionState restoredState) {
        LocalSessionActions localSessionActions = new LocalSessionActions(
                saveLocalSessionAction,
                loadLocalSessionAction
        );
        return new Game(MonopolyRuntime.get(), restoredState, localSessionActions);
    }

    void flushPendingChanges() {
        MonopolyRuntime.get().eventBus().flushPendingChanges();
    }
}
