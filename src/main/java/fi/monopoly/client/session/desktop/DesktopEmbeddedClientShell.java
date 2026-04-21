package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.host.session.local.EmbeddedDesktopSessionHost;
import fi.monopoly.client.session.local.LocalDesktopClientSession;
import fi.monopoly.presentation.game.desktop.session.DesktopHostedGameTestAccess;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;

import java.util.Objects;
import java.util.function.Function;

/**
 * Desktop client shell around the current embedded local session host.
 *
 * <p>This keeps the embedded-host wiring out of {@code MonopolyApp}. The Processing app now talks
 * to one desktop client shell that owns session bootstrap, frame advancement, persistence
 * feedback routing, and current local test hooks.</p>
 */
public final class DesktopEmbeddedClientShell {
    private final EmbeddedDesktopSessionHost embeddedSessionHost;
    private final ClientSession clientSession;
    private final DesktopClientSessionController clientSessionController;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopEmbeddedClientShell(
            DesktopSessionHostCoordinator.Hooks hostHooks,
            Function<ClientSession, ClientSessionFeedbackSink> feedbackSinkFactory
    ) {
        this.embeddedSessionHost = new EmbeddedDesktopSessionHost(Objects.requireNonNull(hostHooks));
        this.clientSession = new LocalDesktopClientSession(embeddedSessionHost);
        this.testAccess = embeddedSessionHost.testAccess();
        ClientSessionFeedbackSink feedbackSink = Objects.requireNonNull(feedbackSinkFactory).apply(clientSession);
        this.clientSessionController = new DesktopClientSessionController(clientSession, feedbackSink);
    }

    public void startFreshSession() {
        clientSessionController.startFreshSession();
    }

    public void advanceFrame() {
        clientSessionController.advanceFrame();
    }

    public ClientSessionView currentView() {
        return clientSessionController.currentView();
    }

    public void saveLocalSession() {
        clientSessionController.saveLocalSession();
    }

    public void loadLocalSession() {
        clientSessionController.loadLocalSession();
    }

    public ClientSession clientSession() {
        return clientSession;
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }
}
