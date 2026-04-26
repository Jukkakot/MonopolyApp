package fi.monopoly.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionUpdates;
import fi.monopoly.client.session.SessionCommandPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Minimal HTTP server that exposes the embedded session host over a local network.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /command} — submit a {@link SessionCommand} encoded as JSON</li>
 *   <li>{@code GET  /snapshot} — retrieve the current {@link ClientSessionSnapshot} as JSON (poll)</li>
 *   <li>{@code GET  /events} — Server-Sent Events stream; pushes a snapshot after each change</li>
 *   <li>{@code GET  /health} — liveness check, always returns 200 OK</li>
 * </ul>
 *
 * <p>Command JSON must include a {@code "type"} discriminator that matches the simple class name
 * prefix (e.g. {@code "RollDice"} for {@code RollDiceCommand}).</p>
 *
 * <p>The SSE endpoint requires {@link ClientSessionUpdates} at construction time. When a new
 * snapshot arrives the server pushes it to all currently-connected SSE clients as a
 * {@code data:} line.</p>
 */
public final class SessionHttpServer {

    private static final Logger log = LoggerFactory.getLogger(SessionHttpServer.class);

    private final SessionCommandPort commandPort;
    private final ClientSessionUpdates sessionUpdates;
    private final Supplier<ClientSessionSnapshot> snapshotSupplier;
    private final int port;
    private final ObjectMapper objectMapper;
    private final SessionCommandMapper commandMapper;

    /** One queue per open SSE connection; snapshot publishing puts to all of them. */
    private final Map<OutputStream, BlockingQueue<ClientSessionSnapshot>> sseQueues =
            new ConcurrentHashMap<>();

    private final ClientSessionListener sseListener = snapshot -> {
        for (BlockingQueue<ClientSessionSnapshot> queue : sseQueues.values()) {
            queue.offer(snapshot);
        }
    };

    private HttpServer httpServer;

    /**
     * @param commandPort     handles incoming commands
     * @param sessionUpdates  source of snapshot push events for the SSE endpoint
     * @param snapshotSupplier provides the current snapshot for poll and initial SSE push
     * @param port            local TCP port to listen on
     */
    public SessionHttpServer(
            SessionCommandPort commandPort,
            ClientSessionUpdates sessionUpdates,
            Supplier<ClientSessionSnapshot> snapshotSupplier,
            int port
    ) {
        this.commandPort = commandPort;
        this.sessionUpdates = sessionUpdates;
        this.snapshotSupplier = snapshotSupplier;
        this.port = port;
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.commandMapper = new SessionCommandMapper(objectMapper);
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/command", this::handleCommand);
        httpServer.createContext("/snapshot", this::handleSnapshot);
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/health", this::handleHealth);
        // Thread pool so that blocking SSE handler threads do not stall other requests.
        httpServer.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "session-http");
            t.setDaemon(true);
            return t;
        }));
        sessionUpdates.addListener(sseListener);
        httpServer.start();
        log.info("Session HTTP server started on port {}", port);
    }

    public void stop() {
        if (httpServer != null) {
            sessionUpdates.removeListener(sseListener);
            httpServer.stop(0);
            log.info("Session HTTP server stopped");
        }
    }

    public int port() {
        return port;
    }

    // -------------------------------------------------------------------------
    // Handlers
    // -------------------------------------------------------------------------

    private void handleCommand(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            SessionCommand command = commandMapper.fromJson(body);
            CommandResult result = commandPort.handle(command);
            String responseJson = objectMapper.writeValueAsString(
                    new CommandResultView(result.accepted(), result.rejections()));
            sendResponse(exchange, result.accepted() ? 200 : 422, responseJson);
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            log.error("Error handling command", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleSnapshot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            ClientSessionSnapshot snapshot = snapshotSupplier.get();
            String responseJson = objectMapper.writeValueAsString(snapshot);
            sendResponse(exchange, 200, responseJson);
        } catch (Exception e) {
            log.error("Error serializing snapshot", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    /**
     * Server-Sent Events endpoint. Blocks until the client disconnects or the server stops.
     * Sends an initial snapshot immediately, then one event per snapshot change.
     * A 30-second heartbeat comment keeps the connection alive through proxies.
     */
    private void handleEvents(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        BlockingQueue<ClientSessionSnapshot> queue = new LinkedBlockingQueue<>();
        sseQueues.put(out, queue);
        queue.offer(snapshotSupplier.get());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ClientSessionSnapshot snapshot = queue.poll(30, TimeUnit.SECONDS);
                if (snapshot == null) {
                    // heartbeat to keep the connection alive
                    out.write(": heartbeat\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } else {
                    String json = objectMapper.writeValueAsString(snapshot);
                    out.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            // client disconnected
        } finally {
            sseQueues.remove(out);
            try { out.close(); } catch (IOException ignored) {}
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "{\"status\":\"ok\"}");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record CommandResultView(
            boolean accepted,
            java.util.List<fi.monopoly.application.result.CommandRejection> rejections
    ) {}
}
