package fi.monopoly.client.desktop;

import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.desktop.DesktopClientSessionModel;
import fi.monopoly.client.session.desktop.DesktopClientSessionRuntime;
import fi.monopoly.client.session.desktop.DesktopClientViewModels;
import fi.monopoly.client.session.desktop.DesktopSessionRenderView;
import fi.monopoly.components.event.MonopolyEventBus;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;

/**
 * Desktop app-side shell around the embedded local client session.
 *
 * <p>This keeps session/bootstrap ownership out of {@link MonopolyApp} itself so the Processing
 * sketch can stay focused on window lifecycle and drawing. The shell still runs locally in-process,
 * but it centralizes the remaining desktop-host wiring behind one app-facing adapter.</p>
 */
public final class DesktopAppShell {
    private final DesktopRuntimeAccess runtimeAccess;
    private final DesktopClientViewModels viewModels;
    private final DesktopClientSessionRuntime sessionRuntime;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopAppShell(MonopolyApp app) {
        DesktopClientHostBinding binding = new EmbeddedLocalDesktopClientBindingFactory().create(
                app,
                this::saveLocalSession,
                this::loadLocalSession
        );
        this.runtimeAccess = binding.runtimeAccess();
        this.viewModels = binding.viewModels();
        this.sessionRuntime = binding.sessionRuntime();
        this.testAccess = binding.testAccess();
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
        return runtimeAccess.runtimeOrNull();
    }

    public MonopolyRuntime runtime() {
        return runtimeAccess.runtime();
    }

    public MonopolyEventBus eventBusOrNull() {
        return runtimeAccess.eventBusOrNull();
    }

    public MonopolyEventBus eventBus() {
        return runtimeAccess.eventBus();
    }
}
