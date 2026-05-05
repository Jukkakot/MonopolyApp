package fi.monopoly.client.desktop;

import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.desktop.*;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.host.session.local.EmbeddedDesktopSessionHost;
import fi.monopoly.presentation.game.desktop.assembly.DefaultDesktopHostedGameFactory;
import fi.monopoly.server.session.EmbeddedSessionServer;
import fi.monopoly.server.session.SessionRegistry;
import fi.monopoly.server.session.SessionServer;
import fi.monopoly.server.transport.SessionHttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

/**
 * Embedded/local desktop client binding factory for the current single-process app.
 *
 * <p>This is the only place where the app still knows how to build the in-process local host
 * graph. The app shell receives only the resulting client-side binding so a future remote-backed
 * binding can replace this factory without changing the shell.</p>
 */
@Slf4j
public final class EmbeddedLocalDesktopClientBindingFactory implements DesktopClientHostBindingFactory {

    /** Override auto-detected port with {@code -Dmonopoly.http.port=8080}. */
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
        ClientSessionFeedbackSink feedbackSink =
                new LocalSessionPersistenceUiHooks(embeddedSessionHost, runtimeBridge::runtime);
        DesktopClientSessionRuntime sessionRuntime = new DesktopClientSessionController(
                sessionUpdates,
                embeddedSessionHost::advanceHostFrame,
                embeddedSessionHost,
                viewModels.sessionModel(),
                viewModels.renderModel(),
                embeddedSessionHost,
                feedbackSink
        );
        DesktopHostedGameTestAccess testAccess = embeddedSessionHost.testAccess();
        startHttpServer(embeddedSessionHost);
        return new DesktopClientHostBinding(runtimeBridge, viewModels, sessionRuntime, testAccess);
    }

    /**
     * Always starts an HTTP server exposing the embedded session. Port is auto-detected unless
     * overridden with {@code -Dmonopoly.http.port=N}. A {@link SessionRegistry} is also attached
     * so external clients can create independent pure-domain sessions via {@code POST /sessions}.
     */
    private static void startHttpServer(EmbeddedDesktopSessionHost host) {
        int port = resolvePort();
        SessionRegistry registry = new SessionRegistry();
        SessionHttpServer httpServer = new SessionHttpServer(
                host, host, host::currentSnapshot, port, registry);
        try {
            httpServer.start();
            Runtime.getRuntime().addShutdownHook(
                    Thread.ofVirtual().name("local-session-server-shutdown").unstarted(httpServer::stop));
            log.info("Session HTTP server started — http://localhost:{} (legacy session + /sessions registry)", port);
        } catch (IOException e) {
            log.warn("Could not start HTTP server on port {} — continuing without HTTP exposure: {}", port, e.getMessage());
        }
    }

    private static int resolvePort() {
        String portProp = System.getProperty(HTTP_PORT_PROPERTY);
        if (portProp != null) {
            try {
                return Integer.parseInt(portProp);
            } catch (NumberFormatException e) {
                log.warn("Invalid {}: '{}' — using auto-detected port", HTTP_PORT_PROPERTY, portProp);
            }
        }
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            return 8080;
        }
    }

    /**
     * Local adapter that exposes only the client-facing session update stream from the embedded
     * host bundle.
     */
    private record LocalClientSessionUpdates(EmbeddedDesktopSessionHost hostedSession) implements ClientSessionUpdates {

        @Override
        public void addListener(ClientSessionListener listener) {
            hostedSession.addListener(listener);
        }

        @Override
        public void removeListener(ClientSessionListener listener) {
            hostedSession.removeListener(listener);
        }
    }
}
