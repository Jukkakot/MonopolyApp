package fi.monopoly.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.SessionCommandPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * Minimal HTTP server that exposes the embedded session host over a local network.
 *
 * <p>Endpoints:</p>
 * <ul>
 *   <li>{@code POST /command} — submit a {@link SessionCommand} encoded as JSON</li>
 *   <li>{@code GET  /snapshot} — retrieve the current {@link ClientSessionSnapshot} as JSON</li>
 *   <li>{@code GET  /health} — liveness check, always returns 200 OK</li>
 * </ul>
 *
 * <p>Command JSON must include a {@code "type"} discriminator that matches the simple class name
 * prefix (e.g. {@code "RollDice"} for {@code RollDiceCommand}).</p>
 */
public final class SessionHttpServer {

    private static final Logger log = LoggerFactory.getLogger(SessionHttpServer.class);

    private final SessionCommandPort commandPort;
    private final Supplier<ClientSessionSnapshot> snapshotSupplier;
    private final int port;
    private final ObjectMapper objectMapper;
    private final SessionCommandMapper commandMapper;

    private HttpServer httpServer;

    public SessionHttpServer(
            SessionCommandPort commandPort,
            Supplier<ClientSessionSnapshot> snapshotSupplier,
            int port
    ) {
        this.commandPort = commandPort;
        this.snapshotSupplier = snapshotSupplier;
        this.port = port;
        this.objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        this.commandMapper = new SessionCommandMapper(objectMapper);
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/command", this::handleCommand);
        httpServer.createContext("/snapshot", this::handleSnapshot);
        httpServer.createContext("/health", this::handleHealth);
        httpServer.setExecutor(null);
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

    private void handleHealth(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "{\"status\":\"ok\"}");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
