package fi.monopoly.client.desktop;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.*;
import fi.monopoly.host.session.local.DesktopHostedGame;
import fi.monopoly.host.session.local.DesktopHostedGameTestAccess;
import fi.monopoly.presentation.remote.RemoteSessionBoardView;
import fi.monopoly.server.session.EmbeddedSessionServer;
import fi.monopoly.server.transport.HttpClientSessionUpdates;
import fi.monopoly.server.transport.HttpSessionCommandPort;
import lombok.extern.slf4j.Slf4j;
import processing.core.PApplet;

import java.io.IOException;
import java.util.List;

/**
 * Desktop client binding factory for HTTP-backed pure-domain session mode.
 *
 * <p>Starts an embedded {@link EmbeddedSessionServer} on a free port, creates a pure-domain
 * session via the server's registry, then wires a {@link RemoteSessionBoardView} against the
 * session's HTTP endpoints. The desktop app renders entirely from received {@link
 * fi.monopoly.domain.session.SessionState} snapshots — no local game-loop authority.</p>
 *
 * <p>Save/load and bot scheduling are not available in this mode; the game runs as a pure client.</p>
 */
@Slf4j
public final class HttpBackedDesktopClientBindingFactory implements DesktopClientHostBindingFactory {

    private static final List<String> DEFAULT_PLAYER_NAMES = List.of("Jukka", "Botti");
    private static final List<String> DEFAULT_COLORS = List.of("#E63946", "#2A9D8F", "#E9C46A", "#F4A261");

    @Override
    public DesktopClientHostBinding create(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction) {
        EmbeddedSessionServer server = startServer();
        String sessionId = server.create(DEFAULT_PLAYER_NAMES, DEFAULT_COLORS);
        String baseUrl = server.baseUrl();

        HttpSessionCommandPort commandPort = HttpSessionCommandPort.forSession(baseUrl, sessionId);
        HttpClientSessionUpdates sessionUpdates = HttpClientSessionUpdates.forSession(baseUrl, sessionId);
        sessionUpdates.connect();

        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();

        RemoteSessionBoardView boardView = new RemoteSessionBoardView(
                (PApplet) app, sessionModel, commandPort, sessionId);

        DesktopSessionViewPort viewPort = () -> boardView;
        DesktopLocalSessionControls noopControls = new NoopLocalSessionControls();
        ClientSessionFeedbackSink noopFeedback = result -> {};

        DesktopClientSessionRuntime sessionRuntime = new DesktopClientSessionController(
                sessionUpdates,
                () -> {},          // no-op frame driver — server owns game logic
                viewPort,
                sessionModel,
                renderModel,
                noopControls,
                noopFeedback
        );

        DesktopHostedGameTestAccess noopTestAccess = new DesktopHostedGameTestAccess(new NoopHostedGameAccess());

        Runtime.getRuntime().addShutdownHook(
                Thread.ofVirtual().name("http-session-server-shutdown").unstarted(server::stop));

        log.info("HTTP-backed session created — sid={} url={}", sessionId.substring(0, 8), baseUrl);

        return new DesktopClientHostBinding(
                new NoopRuntimeAccess(),
                new DesktopClientViewModels(sessionModel, renderModel),
                sessionRuntime,
                noopTestAccess
        );
    }

    private static EmbeddedSessionServer startServer() {
        try {
            return EmbeddedSessionServer.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start embedded session server", e);
        }
    }

    // -------------------------------------------------------------------------
    // No-op stubs for unsupported features in remote mode
    // -------------------------------------------------------------------------

    private static final class NoopLocalSessionControls implements DesktopLocalSessionControls {
        private static final LocalSessionPersistenceResult UNSUPPORTED =
                new LocalSessionPersistenceResult(false, "Tallennus ei ole käytössä etätilassa", null);

        @Override
        public void startFreshSession() {}

        @Override
        public LocalSessionPersistenceResult saveLocalSession() {
            return UNSUPPORTED;
        }

        @Override
        public LocalSessionPersistenceResult loadLocalSession() {
            return UNSUPPORTED;
        }

        @Override
        public void showPersistenceNotice(String message) {}
    }

    private static final class NoopRuntimeAccess implements DesktopRuntimeAccess {
        @Override
        public MonopolyRuntime runtimeOrNull() {
            return null;
        }

        @Override
        public MonopolyRuntime runtime() {
            throw new UnsupportedOperationException("Legacy runtime is not available in HTTP-backed mode");
        }
    }

    private static final class NoopHostedGameAccess implements DesktopHostedGameTestAccess.HostedGameAccess {
        @Override
        public DesktopHostedGame currentHostedGame() {
            return null;
        }

        @Override
        public void setHostedGame(DesktopHostedGame game) {}
    }
}
