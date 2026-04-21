package fi.monopoly.host.session.local;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceUseCase;
import fi.monopoly.application.session.persistence.SessionPersistenceService;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.local.LocalDesktopClientSession;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGame;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;

/**
 * Embedded local session host for the current single-process desktop client.
 *
 * <p>This is the first explicit host object in the codebase. It still runs in the same process as
 * the Processing client, but it groups local session ownership behind one host abstraction instead
 * of letting {@code MonopolyApp} coordinate host concerns directly. That makes the later jump to a
 * real remote host much narrower.</p>
 */
public final class EmbeddedDesktopSessionHost {
    private final DesktopSessionHostCoordinator desktopSessionHostCoordinator;
    private final LocalDesktopClientSession clientSession;

    public EmbeddedDesktopSessionHost(DesktopSessionHostCoordinator.Hooks hooks) {
        this.desktopSessionHostCoordinator = new DesktopSessionHostCoordinator(hooks);
        this.clientSession = new LocalDesktopClientSession(
                desktopSessionHostCoordinator,
                new LocalSessionPersistenceUseCase(
                        new SessionPersistenceService(),
                        desktopSessionHostCoordinator
                )
        );
    }

    public ClientSession clientSession() {
        return clientSession;
    }

    public DesktopHostedGame currentGameForTest() {
        return clientSession.currentGameForTest();
    }

    public void setGameForTest(DesktopHostedGame game) {
        clientSession.setGameForTest(game);
    }
}
