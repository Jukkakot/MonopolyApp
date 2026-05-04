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
import fi.monopoly.server.session.SessionCommandPublisher;
import fi.monopoly.server.session.SessionRegistry;
import fi.monopoly.server.session.SessionSummary;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Minimal HTTP server exposing one or more game sessions over a local network.
 *
 * <h2>Single-session endpoints (backward-compatible)</h2>
 * <ul>
 *   <li>{@code POST /command} — submit a {@link SessionCommand} as JSON</li>
 *   <li>{@code GET  /snapshot} — current {@link ClientSessionSnapshot} (poll)</li>
 *   <li>{@code GET  /events} — SSE stream of snapshot updates</li>
 *   <li>{@code GET  /health} — liveness check</li>
 * </ul>
 *
 * <h2>Multi-session endpoints (active when a {@link SessionRegistry} is provided)</h2>
 * <ul>
 *   <li>{@code POST /sessions} — create a new session</li>
 *   <li>{@code GET  /sessions} — list all sessions</li>
 *   <li>{@code POST /sessions/{id}/command} — submit command to session</li>
 *   <li>{@code GET  /sessions/{id}/snapshot} — snapshot for session</li>
 *   <li>{@code GET  /sessions/{id}/events} — SSE stream for session</li>
 * </ul>
 *
 * <p>All responses include CORS headers. OPTIONS preflight requests return 204 No Content.</p>
 */
@Slf4j
public final class SessionHttpServer {

    private final SessionCommandPort commandPort;
    private final ClientSessionUpdates sessionUpdates;
    private final Supplier<ClientSessionSnapshot> snapshotSupplier;
    private final int port;
    private final ObjectMapper objectMapper;
    private final SessionCommandMapper commandMapper;
    private final SessionRegistry registry;

    private HttpServer httpServer;

    /**
     * Single-session constructor (backward-compatible). No {@code /sessions} endpoints.
     */
    public SessionHttpServer(
            SessionCommandPort commandPort,
            ClientSessionUpdates sessionUpdates,
            Supplier<ClientSessionSnapshot> snapshotSupplier,
            int port
    ) {
        this(commandPort, sessionUpdates, snapshotSupplier, port, null);
    }

    /**
     * Multi-session constructor. Activates {@code /sessions} endpoints alongside the
     * single-session backward-compat endpoints.
     */
    public SessionHttpServer(
            SessionCommandPort commandPort,
            ClientSessionUpdates sessionUpdates,
            Supplier<ClientSessionSnapshot> snapshotSupplier,
            int port,
            SessionRegistry registry
    ) {
        this.commandPort = commandPort;
        this.sessionUpdates = sessionUpdates;
        this.snapshotSupplier = snapshotSupplier;
        this.port = port;
        this.registry = registry;
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.commandMapper = new SessionCommandMapper(objectMapper);
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/command", this::handleCommand);
        httpServer.createContext("/snapshot", this::handleSnapshot);
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/health", this::handleHealth);
        if (registry != null) {
            httpServer.createContext("/sessions", this::handleSessions);
        }
        httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        httpServer.start();
        log.info("Session HTTP server started on port {}", port);
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(0);
            log.info("Session HTTP server stopped");
        }
    }

    public int port() {
        return port;
    }

    // -------------------------------------------------------------------------
    // Single-session handlers (backward-compat)
    // -------------------------------------------------------------------------

    private void handleCommand(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { handleCorsOptions(exchange); return; }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        handleCommandFor(exchange, commandPort);
    }

    private void handleSnapshot(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { handleCorsOptions(exchange); return; }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        handleSnapshotFor(exchange, snapshotSupplier);
    }

    private void handleEvents(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { handleCorsOptions(exchange); return; }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        handleEventsFor(exchange, sessionUpdates, snapshotSupplier);
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { handleCorsOptions(exchange); return; }
        sendResponse(exchange, 200, "{\"status\":\"ok\"}");
    }

    // -------------------------------------------------------------------------
    // Multi-session handler
    // -------------------------------------------------------------------------

    private void handleSessions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { handleCorsOptions(exchange); return; }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Strip /sessions prefix and split remainder
        String sub = path.substring("/sessions".length()); // "", "/", "/{id}/command", etc.

        if (sub.isEmpty() || sub.equals("/")) {
            if ("GET".equalsIgnoreCase(method)) handleSessionsList(exchange);
            else if ("POST".equalsIgnoreCase(method)) handleSessionsCreate(exchange);
            else sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        // Expect /{id}/{resource}
        String[] parts = sub.split("/", 3); // ["", "{id}", "command|snapshot|events"]
        if (parts.length < 3) {
            sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }
        String sessionId = parts[1];
        String resource = parts[2];

        Optional<SessionCommandPublisher> publisherOpt = registry.get(sessionId);
        if (publisherOpt.isEmpty()) {
            sendResponse(exchange, 404, "{\"error\":\"Session not found\"}");
            return;
        }
        SessionCommandPublisher publisher = publisherOpt.get();

        switch (resource) {
            case "command" -> {
                if (!"POST".equalsIgnoreCase(method)) { sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }
                handleCommandFor(exchange, publisher);
            }
            case "snapshot" -> {
                if (!"GET".equalsIgnoreCase(method)) { sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }
                handleSnapshotFor(exchange, publisher::currentSnapshot);
            }
            case "events" -> {
                if (!"GET".equalsIgnoreCase(method)) { sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}"); return; }
                handleEventsFor(exchange, publisher, publisher::currentSnapshot);
            }
            default -> sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
        }
    }

    private void handleSessionsList(HttpExchange exchange) throws IOException {
        try {
            List<SessionSummary> summaries = registry.list();
            sendResponse(exchange, 200, objectMapper.writeValueAsString(summaries));
        } catch (Exception e) {
            log.error("Error listing sessions", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleSessionsCreate(HttpExchange exchange) throws IOException {
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            Map<?, ?> request = objectMapper.readValue(body, Map.class);
            @SuppressWarnings("unchecked")
            List<String> names = (List<String>) request.get("names");
            @SuppressWarnings("unchecked")
            Object rawColors = request.get("colors");
            @SuppressWarnings("unchecked")
            List<String> colors = rawColors != null ? (List<String>) rawColors : List.of();
            if (names == null || names.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"names is required\"}");
                return;
            }
            String sessionId = registry.create(names, colors);
            sendResponse(exchange, 201, "{\"sessionId\":\"" + sessionId + "\"}");
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"" + jsonEscape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            log.error("Error creating session", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    // -------------------------------------------------------------------------
    // Shared per-session helpers
    // -------------------------------------------------------------------------

    private void handleCommandFor(HttpExchange exchange, SessionCommandPort port) throws IOException {
        try {
            byte[] body = exchange.getRequestBody().readAllBytes();
            SessionCommand command = commandMapper.fromJson(body);
            CommandResult result = port.handle(command);
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

    private void handleSnapshotFor(HttpExchange exchange, Supplier<ClientSessionSnapshot> supplier) throws IOException {
        try {
            sendResponse(exchange, 200, objectMapper.writeValueAsString(supplier.get()));
        } catch (Exception e) {
            log.error("Error serializing snapshot", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    /**
     * SSE endpoint. Each call registers its own listener with {@code updates} so that only
     * snapshots from that specific session are delivered to this connection.
     * A 30-second heartbeat comment keeps proxies from closing idle connections.
     */
    private void handleEventsFor(
            HttpExchange exchange,
            ClientSessionUpdates updates,
            Supplier<ClientSessionSnapshot> supplier
    ) throws IOException {
        addCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Content-Type", "text/event-stream; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.sendResponseHeaders(200, 0);
        OutputStream out = exchange.getResponseBody();
        BlockingQueue<ClientSessionSnapshot> queue = new LinkedBlockingQueue<>();
        ClientSessionListener listener = snapshot -> queue.offer(snapshot);
        updates.addListener(listener);
        queue.offer(supplier.get());
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ClientSessionSnapshot snapshot = queue.poll(30, TimeUnit.SECONDS);
                if (snapshot == null) {
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
            updates.removeListener(listener);
            try { out.close(); } catch (IOException ignored) {}
        }
    }

    // -------------------------------------------------------------------------
    // CORS helpers
    // -------------------------------------------------------------------------

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void handleCorsOptions(HttpExchange exchange) throws IOException {
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(204, -1);
    }

    // -------------------------------------------------------------------------
    // Response helper
    // -------------------------------------------------------------------------

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        addCorsHeaders(exchange);
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
