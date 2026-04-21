package fi.monopoly;

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
final class MonopolyDesktopAppShell {
    private final DesktopEmbeddedClientShell desktopClientShell;

    MonopolyDesktopAppShell(MonopolyApp app) {
        MonopolyDesktopRuntimeBridge runtimeBridge = new MonopolyDesktopRuntimeBridge(
                app,
                this::saveLocalSession,
                this::loadLocalSession
        );
        this.desktopClientShell = new DesktopEmbeddedClientShell(
                runtimeBridge,
                MonopolyLocalSessionPersistenceUiHooks::new
        );
    }

    void startFreshSession() {
        desktopClientShell.startFreshSession();
    }

    void advanceFrame() {
        desktopClientShell.advanceFrame();
    }

    ClientSessionView currentView() {
        return desktopClientShell.currentView();
    }

    void saveLocalSession() {
        desktopClientShell.saveLocalSession();
    }

    void loadLocalSession() {
        desktopClientShell.loadLocalSession();
    }

    Game currentGameForTest() {
        return desktopClientShell.currentGameForTest();
    }

    void setGameForTest(Game game) {
        desktopClientShell.setGameForTest(game);
    }
}
