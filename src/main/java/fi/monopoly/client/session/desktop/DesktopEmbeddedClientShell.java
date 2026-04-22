package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionView;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.host.session.local.DesktopSessionHostCoordinator;
import fi.monopoly.host.session.local.EmbeddedDesktopSessionHost;

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
    private final DesktopSessionFrameDriver frameDriver;
    private final DesktopLocalSessionControls localSessionControls;
    private final DesktopClientSessionRuntime sessionRuntime;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopEmbeddedClientShell(
            DesktopSessionHostCoordinator.Hooks hostHooks,
            Function<DesktopLocalSessionControls, ClientSessionFeedbackSink> feedbackSinkFactory
    ) {
        this.embeddedSessionHost = new EmbeddedDesktopSessionHost(Objects.requireNonNull(hostHooks));
        this.clientSession = embeddedSessionHost;
        this.frameDriver = embeddedSessionHost::advanceHostFrame;
        this.localSessionControls = embeddedSessionHost;
        this.testAccess = embeddedSessionHost.testAccess();
        ClientSessionFeedbackSink feedbackSink = Objects.requireNonNull(feedbackSinkFactory).apply(localSessionControls);
        this.sessionRuntime = new DesktopClientSessionController(clientSession, frameDriver, localSessionControls, feedbackSink);
    }

    public ClientSession clientSession() {
        return clientSession;
    }

    public DesktopClientSessionRuntime runtime() {
        return sessionRuntime;
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }
}
