package fi.monopoly.client.session;

/**
 * Transitional client-facing session seam for whichever host currently owns Monopoly session
 * authority.
 *
 * <p>The current desktop client still talks to an embedded local host in-process, but this
 * interface is intentionally shaped as a client dependency rather than a direct dependency on the
 * local desktop host coordinator. The same surface should remain valid when a remote backend host
 * is introduced later.</p>
 */
public interface ClientSession {
    void addListener(ClientSessionListener listener);

    void removeListener(ClientSessionListener listener);
}
