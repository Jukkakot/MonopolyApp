package fi.monopoly.client.desktop;

import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.client.session.desktop.DesktopEmbeddedClientShell;
import fi.monopoly.presentation.game.desktop.assembly.DefaultDesktopHostedGameFactory;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGameTestAccess;

/**
 * Desktop app-side shell around the embedded local client session.
 *
 * <p>This keeps session/bootstrap ownership out of {@link MonopolyApp} itself so the Processing
 * sketch can stay focused on window lifecycle and drawing. The shell still runs locally in-process,
 * but it centralizes the remaining desktop-host wiring behind one app-facing adapter.</p>
 */
public final class DesktopAppShell {
    private final DesktopEmbeddedClientShell desktopClientShell;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopAppShell(MonopolyApp app) {
        DesktopRuntimeBridge runtimeBridge = new DesktopRuntimeBridge(
                app,
                this::saveLocalSession,
                this::loadLocalSession,
                new DefaultDesktopHostedGameFactory()
        );
        this.desktopClientShell = new DesktopEmbeddedClientShell(
                runtimeBridge,
                LocalSessionPersistenceUiHooks::new
        );
        this.testAccess = desktopClientShell.testAccess();
    }

    public void startFreshSession() {
        desktopClientShell.startFreshSession();
    }

    public void advanceFrame() {
        desktopClientShell.advanceFrame();
    }

    public ClientSessionView currentView() {
        return desktopClientShell.currentView();
    }

    public void saveLocalSession() {
        desktopClientShell.saveLocalSession();
    }

    public void loadLocalSession() {
        desktopClientShell.loadLocalSession();
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }
}
