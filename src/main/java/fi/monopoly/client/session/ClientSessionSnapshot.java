package fi.monopoly.client.session;

import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;

/**
 * Small client-visible snapshot of session availability and lifecycle state.
 *
 * <p>This is intentionally smaller than the full authoritative session state. Its job is to give
 * the client a stable host-facing status payload even while rendering still happens through the
 * legacy desktop runtime host.</p>
 */
public record ClientSessionSnapshot(
        String sessionId,
        long version,
        SessionStatus status,
        boolean viewAvailable
) {
    public static ClientSessionSnapshot empty() {
        return new ClientSessionSnapshot(null, 0L, null, false);
    }

    public static ClientSessionSnapshot from(SessionState sessionState, boolean viewAvailable) {
        if (sessionState == null) {
            return empty();
        }
        return new ClientSessionSnapshot(
                sessionState.sessionId(),
                sessionState.version(),
                sessionState.status(),
                viewAvailable
        );
    }
}
