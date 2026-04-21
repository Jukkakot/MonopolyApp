package fi.monopoly.client.desktop;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.assembly.DesktopHostedGameFactory;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGame;
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
final class DesktopRuntimeBridge implements DesktopSessionHostCoordinator.Hooks {
    private final MonopolyApp app;
    private final Runnable saveLocalSessionAction;
    private final Runnable loadLocalSessionAction;
    private final DesktopHostedGameFactory desktopHostedGameFactory;

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
    }

    @Override
    public void shutdownSessionRuntime() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime == null) {
            return;
        }
        runtime.popupService().hideAll();
        runtime.setGameSession(null);
        runtime.eventBus().flushPendingChanges();
        runtime.popupService().hideAll();
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
        MonopolyRuntime.initialize(
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
        return desktopHostedGameFactory.create(MonopolyRuntime.get(), restoredState, localSessionActions);
    }

    @Override
    public void flushPendingChanges() {
        MonopolyRuntime.get().eventBus().flushPendingChanges();
    }

    @Override
    public void disposeGame(DesktopHostedGame game) {
        game.dispose();
    }
}
