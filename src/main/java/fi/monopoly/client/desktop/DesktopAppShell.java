package fi.monopoly.client.desktop;

import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.client.session.desktop.DesktopClientSessionRuntime;
import fi.monopoly.client.session.desktop.DesktopEmbeddedClientShell;
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
    private final DesktopClientSessionRuntime sessionRuntime;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopAppShell(MonopolyApp app) {
        DesktopRuntimeBridge runtimeBridge = new DesktopRuntimeBridge(
                app,
                this::saveLocalSession,
                this::loadLocalSession,
                new DefaultDesktopHostedGameFactory()
        );
        DesktopEmbeddedClientShell desktopClientShell = new DesktopEmbeddedClientShell(
                runtimeBridge,
                LocalSessionPersistenceUiHooks::new
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

    public ClientSessionView currentView() {
        return sessionRuntime.currentView();
    }

    public ClientSessionSnapshot currentSnapshot() {
        return sessionRuntime.currentSnapshot();
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
}
