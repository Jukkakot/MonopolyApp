package fi.monopoly.client.desktop;

import fi.monopoly.MonopolyApp;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.client.session.desktop.DesktopEmbeddedClientShell;
import fi.monopoly.components.Game;

/**
 * Desktop app-side shell around the embedded local client session.
 *
 * <p>This keeps session/bootstrap ownership out of {@link MonopolyApp} itself so the Processing
 * sketch can stay focused on window lifecycle and drawing. The shell still runs locally in-process,
 * but it centralizes the remaining desktop-host wiring behind one app-facing adapter.</p>
 */
public final class DesktopAppShell {
    private final DesktopEmbeddedClientShell desktopClientShell;

    public DesktopAppShell(MonopolyApp app) {
        DesktopRuntimeBridge runtimeBridge = new DesktopRuntimeBridge(
                app,
                this::saveLocalSession,
                this::loadLocalSession
        );
        this.desktopClientShell = new DesktopEmbeddedClientShell(
                runtimeBridge,
                LocalSessionPersistenceUiHooks::new
        );
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

    public Game currentGameForTest() {
        return desktopClientShell.currentGameForTest();
    }

    public void setGameForTest(Game game) {
        desktopClientShell.setGameForTest(game);
    }
}
