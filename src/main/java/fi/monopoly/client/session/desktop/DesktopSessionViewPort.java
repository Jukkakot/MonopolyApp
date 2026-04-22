package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSessionView;

/**
 * Desktop-local live view port for embedded rendering.
 *
 * <p>A remote-ready {@code ClientSession} should expose snapshots and update subscriptions, not a
 * process-local render callback object. Embedded desktop mode still needs that live view, so it
 * crosses this separate local-only seam instead.</p>
 */
public interface DesktopSessionViewPort {
    ClientSessionView currentView();
}
