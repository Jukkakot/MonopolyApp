package fi.monopoly.server.session;

/**
 * Entry point for a future standalone session server process.
 *
 * <p>The standalone server is the next architectural milestone after the embedded HTTP transport
 * MVP in {@code server.transport}. Once pure domain gateway implementations exist (replacing the
 * current {@code presentation.legacy.session.*} adapters that still depend on Processing-era
 * runtime objects), this class will bootstrap a fully self-contained session host:</p>
 *
 * <pre>
 *   // Future standalone startup — pending pure domain gateways:
 *
 *   String sessionId   = UUID.randomUUID().toString();
 *   int    port        = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
 *
 *   SessionApplicationService service = PureDomainSessionFactory.create(sessionId);
 *   SessionCommandPublisher   publisher = new SessionCommandPublisher(service);
 *
 *   SessionServer server = new SessionServer(publisher, publisher,
 *           publisher::currentSnapshot, port);
 *   server.start();
 *   server.registerShutdownHook();
 * </pre>
 *
 * <p><b>Current workaround:</b> run the desktop app from IntelliJ with
 * {@code -Dmonopoly.http.port=8080}. The embedded host exposes the same HTTP API; a remote
 * desktop client can connect with {@code HttpSessionCommandPort} +
 * {@code HttpClientSessionUpdates}.</p>
 *
 * <h2>What is blocking full standalone operation</h2>
 * <ol>
 *   <li>All gateway adapters in {@code presentation.legacy.session} still depend on mutable
 *       Processing-era runtime objects ({@code Players}, {@code Dices}, {@code DebtController},
 *       etc.).</li>
 *   <li>Rule logic (rent calculation, movement, jail handling, auction bidding) still lives in
 *       those legacy objects and must be extracted to pure domain/application layer code first.</li>
 *   <li>{@code PureDomainSessionFactory} — creates a {@link fi.monopoly.application.session.SessionApplicationService}
 *       wired with pure domain gateway implementations (no Processing runtime objects). The
 *       {@link SessionCommandPublisher} wrapper already exists and is ready to use once this
 *       factory is in place.</li>
 * </ol>
 *
 * @see SessionServer
 * @see fi.monopoly.server.transport.SessionHttpServer
 */
public final class StartSessionServer {

    private StartSessionServer() {}

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        System.err.println("""
                Standalone session server is not yet available.

                Pure domain gateway implementations are required before the session host
                can run without the Processing desktop runtime.

                Current workaround: run the desktop app from IntelliJ with
                  -Dmonopoly.http.port=%d
                The embedded host exposes the same HTTP API on that port.
                """.formatted(port));
        System.exit(1);
    }
}
