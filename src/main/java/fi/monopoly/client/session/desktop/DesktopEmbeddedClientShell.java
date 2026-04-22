package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionUpdates;
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
    private final ClientSessionUpdates sessionUpdates;
    private final DesktopSessionFrameDriver frameDriver;
    private final DesktopSessionViewPort viewPort;
    private final DesktopClientViewModels viewModels;
    private final DesktopLocalSessionControls localSessionControls;
    private final DesktopClientSessionRuntime sessionRuntime;
    private final DesktopHostedGameTestAccess testAccess;

    public DesktopEmbeddedClientShell(
            DesktopSessionHostCoordinator.Hooks hostHooks,
            DesktopClientViewModels viewModels,
            Function<DesktopLocalSessionControls, ClientSessionFeedbackSink> feedbackSinkFactory
    ) {
        this.embeddedSessionHost = new EmbeddedDesktopSessionHost(Objects.requireNonNull(hostHooks));
        this.sessionUpdates = embeddedSessionHost;
        this.frameDriver = embeddedSessionHost::advanceHostFrame;
        this.viewPort = embeddedSessionHost;
        this.viewModels = Objects.requireNonNull(viewModels);
        this.localSessionControls = embeddedSessionHost;
        this.testAccess = embeddedSessionHost.testAccess();
        ClientSessionFeedbackSink feedbackSink = Objects.requireNonNull(feedbackSinkFactory).apply(localSessionControls);
        this.sessionRuntime = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                this.viewModels.sessionModel(),
                this.viewModels.renderModel(),
                localSessionControls,
                feedbackSink
        );
    }

    public ClientSessionUpdates sessionUpdates() {
        return sessionUpdates;
    }

    public DesktopClientSessionRuntime runtime() {
        return sessionRuntime;
    }

    public DesktopHostedGameTestAccess testAccess() {
        return testAccess;
    }
}
