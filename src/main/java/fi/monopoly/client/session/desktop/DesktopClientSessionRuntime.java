package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;

/**
 * Client-owned desktop session runtime port used by the Processing app shell.
 *
 * <p>This keeps the app talking to one stable desktop-session runtime abstraction instead of a set
 * of embedded-host-specific forwarding methods. Embedded local mode can still back this runtime
 * with an in-process host, but the app itself only depends on one client-side session control and
 * state surface.</p>
 */
public interface DesktopClientSessionRuntime {
    void startFreshSession();

    void advanceFrame();

    DesktopSessionRenderView currentView();

    ClientSessionSnapshot currentSnapshot();

    void addListener(ClientSessionListener listener);

    void removeListener(ClientSessionListener listener);

    void saveLocalSession();

    void loadLocalSession();
}
