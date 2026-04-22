package fi.monopoly.client.desktop;

import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.desktop.DesktopClientRenderModel;
import fi.monopoly.client.session.desktop.DesktopClientSessionModel;
import fi.monopoly.client.session.desktop.DesktopClientSessionRuntime;
import fi.monopoly.client.session.desktop.DesktopClientViewModels;
import fi.monopoly.client.session.desktop.DesktopEmbeddedClientShell;
import fi.monopoly.client.session.desktop.DesktopSessionRenderView;
import fi.monopoly.components.event.MonopolyEventBus;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.presentation.game.desktop.assembly.DefaultDesktopHostedGameFactory;

/**
 * Desktop app-side shell around the embedded local client session.
 *
 * <p>This keeps session/bootstrap ownership out of {@link MonopolyApp} itself so the Processing
 * sketch can stay focused on window lifecycle and drawing. The shell still runs locally in-process,
 * but it centralizes the remaining desktop-host wiring behind one app-facing adapter.</p>
 */
public final class DesktopAppShell {
    private final DesktopRuntimeBridge runtimeBridge;
    private final DesktopClientViewModels viewModels;
    private final DesktopClientSessionRuntime sessionRuntime;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopAppShell(MonopolyApp app) {
        this.runtimeBridge = new DesktopRuntimeBridge(
                app,
                this::saveLocalSession,
                this::loadLocalSession,
                new DefaultDesktopHostedGameFactory()
        );
        this.viewModels = new DesktopClientViewModels(
                new DesktopClientSessionModel(),
                new DesktopClientRenderModel()
        );
        DesktopEmbeddedClientShell desktopClientShell = new DesktopEmbeddedClientShell(
                runtimeBridge,
                viewModels,
                clientSession -> new LocalSessionPersistenceUiHooks(clientSession, runtimeBridge::runtime)
        );
        this.sessionRuntime = desktopClientShell.runtime();
        this.testAccess = desktopClientShell.testAccess();
    }

    public void startFreshSession() {
        sessionRuntime.startFreshSession();
    }

    public void advanceFrame() {
        sessionRuntime.advanceFrame();
    }

    public DesktopSessionRenderView currentView() {
        return viewModels.renderModel().currentView();
    }

    public ClientSessionSnapshot currentSnapshot() {
        return viewModels.sessionModel().currentSnapshot();
    }

    public DesktopClientSessionModel sessionModel() {
        return viewModels.sessionModel();
    }

    public void saveLocalSession() {
        sessionRuntime.saveLocalSession();
    }

    public void loadLocalSession() {
        sessionRuntime.loadLocalSession();
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }

    public MonopolyRuntime runtimeOrNull() {
        return runtimeBridge.runtimeOrNull();
    }

    public MonopolyRuntime runtime() {
        return runtimeBridge.runtime();
    }

    public MonopolyEventBus eventBusOrNull() {
        MonopolyRuntime runtime = runtimeOrNull();
        return runtime != null ? runtime.eventBus() : null;
    }

    public MonopolyEventBus eventBus() {
        return runtime().eventBus();
    }
}
