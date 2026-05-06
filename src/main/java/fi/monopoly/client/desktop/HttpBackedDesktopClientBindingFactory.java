package fi.monopoly.client.desktop;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.client.session.desktop.*;
import fi.monopoly.domain.session.SeatKind;
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
    private static final List<SeatKind> DEFAULT_SEAT_KINDS = List.of(SeatKind.HUMAN, SeatKind.BOT);

    /**
     * Reads player configuration from system properties.
     *
     * <p>Supported properties:
     * <ul>
     *   <li>{@code monopoly.players} — comma-separated names, e.g. {@code Jukka,Mari,Pekka}</li>
     *   <li>{@code monopoly.bots} — comma-separated 0/1 flags matching players, e.g. {@code 0,1,0}.
     *       Defaults to first seat HUMAN, all others BOT.</li>
     * </ul>
     * If {@code monopoly.players} is absent the defaults above are used.
     */
    private static SessionConfig resolveSessionConfig() {
        String playersProp = System.getProperty("monopoly.players");
        if (playersProp == null || playersProp.isBlank()) {
            return new SessionConfig(DEFAULT_PLAYER_NAMES, DEFAULT_COLORS, DEFAULT_SEAT_KINDS);
        }
        List<String> names = List.of(playersProp.split(",")).stream().map(String::trim).toList();
        String botsProp = System.getProperty("monopoly.bots", "");
        String[] botFlags = botsProp.isBlank() ? new String[0] : botsProp.split(",");
        List<SeatKind> seatKinds = new java.util.ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            boolean isBot = i < botFlags.length
                    ? "1".equals(botFlags[i].trim())
                    : (i > 0);
            seatKinds.add(isBot ? SeatKind.BOT : SeatKind.HUMAN);
        }
        List<String> colors = DEFAULT_COLORS.subList(0, Math.min(names.size(), DEFAULT_COLORS.size()));
        return new SessionConfig(names, colors, seatKinds);
    }

    private record SessionConfig(List<String> names, List<String> colors, List<SeatKind> seatKinds) {}

    @Override
    public DesktopClientHostBinding create(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction) {
        EmbeddedSessionServer server = startServer();
        SessionConfig config = resolveSessionConfig();
        log.info("Remote session config: players={} bots={}",
                config.names(),
                config.seatKinds().stream().map(k -> k == SeatKind.BOT ? "BOT" : "HUMAN").toList());
        String sessionId = server.create(config.names(), config.colors(), config.seatKinds());
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
