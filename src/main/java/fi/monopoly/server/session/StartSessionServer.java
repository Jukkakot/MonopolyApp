package fi.monopoly.server.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.domain.session.SessionState;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Entry point for the standalone session server process.
 *
 * <p>Bootstraps a fully self-contained session host using pure domain gateway implementations
 * (no Processing runtime objects required):</p>
 *
 * <pre>
 *   java -cp monopoly.jar fi.monopoly.server.session.StartSessionServer [port] [player1] [player2] ...
 * </pre>
 *
 * <p>Default port is 8080. Default players are "Pelaaja 1" and "Pelaaja 2".
 * The session starts immediately with the given players and standard board setup.</p>
 *
 * <p>The same HTTP API is available as in embedded mode ({@code -Dmonopoly.http.port}):</p>
 * <ul>
 *   <li>{@code POST /command} — submit a {@code SessionCommand} as JSON</li>
 *   <li>{@code GET /snapshot} — fetch the current {@code ClientSessionSnapshot}</li>
 *   <li>{@code GET /events} — SSE stream of snapshot updates</li>
 *   <li>{@code GET /health} — liveness probe</li>
 * </ul>
 *
 * @see SessionServer
 * @see fi.monopoly.server.transport.SessionHttpServer
 */
@Slf4j
public final class StartSessionServer {

    private StartSessionServer() {}

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        List<String> playerNames = args.length > 1
                ? List.of(args).subList(1, args.length)
                : List.of("Pelaaja 1", "Pelaaja 2");
        List<String> colors = List.of("#E63946", "#2A9D8F", "#E9C46A", "#264653");

        String sessionId = UUID.randomUUID().toString();
        SessionState initialState = PureDomainSessionFactory.initialGameState(sessionId, playerNames, colors);
        SessionApplicationService service = PureDomainSessionFactory.create(sessionId, initialState);
        SessionCommandPublisher publisher = new SessionCommandPublisher(service);

        SessionServer server = new SessionServer(publisher, publisher, publisher::currentSnapshot, port);
        server.start();
        server.registerShutdownHook();

        log.info("Standalone session server started — sessionId={} port={} players={}",
                sessionId, port, playerNames);
    }
}
