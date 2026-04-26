package fi.monopoly.client.desktop;

import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.desktop.DesktopClientRenderModel;
import fi.monopoly.client.session.desktop.DesktopClientSessionController;
import fi.monopoly.client.session.desktop.DesktopClientSessionModel;
import fi.monopoly.client.session.desktop.DesktopClientSessionRuntime;
import fi.monopoly.client.session.desktop.DesktopClientViewModels;
import fi.monopoly.client.session.desktop.DesktopLocalSessionControls;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.host.session.local.EmbeddedDesktopSessionHost;
import fi.monopoly.presentation.game.desktop.assembly.DefaultDesktopHostedGameFactory;
import fi.monopoly.server.transport.SessionHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Embedded/local desktop client binding factory for the current single-process app.
 *
 * <p>This is the only place where the app still knows how to build the in-process local host
 * graph. The app shell receives only the resulting client-side binding so a future remote-backed
 * binding can replace this factory without changing the shell.</p>
 */
public final class EmbeddedLocalDesktopClientBindingFactory implements DesktopClientHostBindingFactory {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedLocalDesktopClientBindingFactory.class);

    /** Set {@code -Dmonopoly.http.port=8080} to expose the session host over HTTP. */
    private static final String HTTP_PORT_PROPERTY = "monopoly.http.port";

    @Override
    public DesktopClientHostBinding create(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction
    ) {
        DesktopRuntimeBridge runtimeBridge = new DesktopRuntimeBridge(
                Objects.requireNonNull(app),
                Objects.requireNonNull(saveLocalSessionAction),
                Objects.requireNonNull(loadLocalSessionAction),
                new DefaultDesktopHostedGameFactory()
        );
        DesktopClientViewModels viewModels = new DesktopClientViewModels(
                new DesktopClientSessionModel(),
                new DesktopClientRenderModel()
        );
        EmbeddedDesktopSessionHost embeddedSessionHost = new EmbeddedDesktopSessionHost(runtimeBridge);
        ClientSessionUpdates sessionUpdates = new LocalClientSessionUpdates(embeddedSessionHost);
        DesktopLocalSessionControls localSessionControls = embeddedSessionHost;
        ClientSessionFeedbackSink feedbackSink =
                new LocalSessionPersistenceUiHooks(localSessionControls, runtimeBridge::runtime);
        DesktopClientSessionRuntime sessionRuntime = new DesktopClientSessionController(
                sessionUpdates,
                embeddedSessionHost::advanceHostFrame,
                embeddedSessionHost,
                viewModels.sessionModel(),
                viewModels.renderModel(),
                localSessionControls,
                feedbackSink
        );
        DesktopHostedGameTestAccess testAccess = embeddedSessionHost.testAccess();
        maybeStartHttpServer(embeddedSessionHost);
        return new DesktopClientHostBinding(runtimeBridge, viewModels, sessionRuntime, testAccess);
    }

    private static void maybeStartHttpServer(EmbeddedDesktopSessionHost host) {
        String portProp = System.getProperty(HTTP_PORT_PROPERTY);
        if (portProp == null) {
            return;
        }
        int port;
        try {
            port = Integer.parseInt(portProp);
        } catch (NumberFormatException e) {
            log.warn("Invalid {}: '{}' — HTTP server not started", HTTP_PORT_PROPERTY, portProp);
            return;
        }
        SessionHttpServer httpServer = new SessionHttpServer(host, host, host::currentSnapshot, port);
        try {
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(new Thread(httpServer::stop, "session-http-shutdown"));
        } catch (IOException e) {
            log.error("Failed to start session HTTP server on port {}", port, e);
        }
    }

    /**
     * Local adapter that exposes only the client-facing session update stream from the embedded
     * host bundle.
     */
    private static final class LocalClientSessionUpdates implements ClientSessionUpdates {
        private final EmbeddedDesktopSessionHost hostedSession;

        private LocalClientSessionUpdates(EmbeddedDesktopSessionHost hostedSession) {
            this.hostedSession = hostedSession;
        }

        @Override
        public void addListener(fi.monopoly.client.session.ClientSessionListener listener) {
            hostedSession.addListener(listener);
        }

        @Override
        public void removeListener(fi.monopoly.client.session.ClientSessionListener listener) {
            hostedSession.removeListener(listener);
        }
    }
}
