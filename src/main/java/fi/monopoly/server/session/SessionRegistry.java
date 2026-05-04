package fi.monopoly.server.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.domain.session.SessionState;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Thread-safe registry of active game sessions for the multi-session server mode.
 *
 * <p>Sessions are created on demand via {@link #create} and identified by a random UUID.
 * The registry owns the {@link SessionCommandPublisher} lifecycle for each session.</p>
 */
public final class SessionRegistry {

    private record Entry(SessionCommandPublisher publisher, List<String> playerNames) {}

    private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();

    /**
     * Creates a new session with the given player names and colours, returns its session ID.
     */
    public String create(List<String> names, List<String> colors) {
        String sessionId = UUID.randomUUID().toString();
        SessionState initialState = PureDomainSessionFactory.initialGameState(sessionId, names, colors);
        SessionApplicationService service = PureDomainSessionFactory.create(sessionId, initialState);
        SessionCommandPublisher publisher = new SessionCommandPublisher(service);
        sessions.put(sessionId, new Entry(publisher, List.copyOf(names)));
        return sessionId;
    }

    public Optional<SessionCommandPublisher> get(String sessionId) {
        Entry entry = sessions.get(sessionId);
        return Optional.ofNullable(entry).map(Entry::publisher);
    }

    public List<SessionSummary> list() {
        return sessions.entrySet().stream()
                .map(e -> new SessionSummary(
                        e.getKey(),
                        e.getValue().playerNames(),
                        e.getValue().publisher().currentState().status()))
                .collect(Collectors.toList());
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }
}
