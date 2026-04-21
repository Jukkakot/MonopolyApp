package fi.monopoly.client.desktop;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.session.local.DesktopHostedGame;
import fi.monopoly.host.session.local.DesktopSessionHostCoordinator;
import fi.monopoly.presentation.game.desktop.assembly.DesktopHostedGameFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Desktop runtime/bootstrap bridge around the current Processing host.
 *
 * <p>This isolates runtime shutdown, control-layer recreation, font application, and local game
 * creation from {@link MonopolyApp}. The app should remain a thin Processing shell while desktop
 * host bootstrap details live here as a dedicated adapter.</p>
 */
@Slf4j
final class DesktopRuntimeBridge implements DesktopSessionHostCoordinator.Hooks {
    private final MonopolyApp app;
    private final Runnable saveLocalSessionAction;
    private final Runnable loadLocalSessionAction;
    private final DesktopHostedGameFactory desktopHostedGameFactory;
    private MonopolyRuntime runtime;

    DesktopRuntimeBridge(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction,
            DesktopHostedGameFactory desktopHostedGameFactory
    ) {
        this.app = app;
        this.saveLocalSessionAction = saveLocalSessionAction;
        this.loadLocalSessionAction = loadLocalSessionAction;
        this.desktopHostedGameFactory = desktopHostedGameFactory;
        this.runtime = MonopolyRuntime.peek();
    }

    @Override
    public void shutdownSessionRuntime() {
        MonopolyRuntime activeRuntime = currentRuntimeOrNull();
        if (activeRuntime == null) {
            return;
        }
        activeRuntime.popupService().hideAll();
        activeRuntime.setGameSession(null);
        activeRuntime.eventBus().flushPendingChanges();
        activeRuntime.popupService().hideAll();
    }

    @Override
    public void disposeControlLayer() {
        if (DesktopRuntimeResources.controlLayer() != null) {
            DesktopRuntimeResources.controlLayer().dispose();
        }
    }

    @Override
    public void initializeControlLayer() {
        DesktopRuntimeResources.setControlLayer(new ControlP5(app));
        runtime = MonopolyRuntime.initialize(
                app,
                DesktopRuntimeResources.controlLayer(),
                DesktopRuntimeResources.font10(),
                DesktopRuntimeResources.font20(),
                DesktopRuntimeResources.font30()
        );
    }

    @Override
    public void applyDefaultTextFont() {
        if (DesktopRuntimeResources.font10() == null) {
            return;
        }
        try {
            app.textFont(DesktopRuntimeResources.font10());
        } catch (RuntimeException e) {
            log.debug("Skipping textFont apply during runtime rebuild because graphics context is not ready yet");
        }
    }

    @Override
    public DesktopHostedGame createGame(SessionState restoredState) {
        LocalSessionActions localSessionActions = new LocalSessionActions(
                saveLocalSessionAction,
                loadLocalSessionAction
        );
        return desktopHostedGameFactory.create(runtime(), restoredState, localSessionActions);
    }

    @Override
    public void flushPendingChanges() {
        runtime().eventBus().flushPendingChanges();
    }

    @Override
    public void disposeGame(DesktopHostedGame game) {
        game.dispose();
    }

    MonopolyRuntime runtime() {
        MonopolyRuntime activeRuntime = currentRuntimeOrNull();
        if (activeRuntime == null) {
            throw new IllegalStateException("Desktop runtime has not been initialized yet");
        }
        return activeRuntime;
    }

    private MonopolyRuntime currentRuntimeOrNull() {
        MonopolyRuntime current = MonopolyRuntime.peek();
        if (current != null && current != runtime) {
            runtime = current;
        }
        return runtime;
    }
}
