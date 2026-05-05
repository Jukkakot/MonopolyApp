package fi.monopoly.presentation.remote;

/**
 * Optional capability for session render views that handle mouse press events.
 *
 * <p>Implement this interface alongside {@link fi.monopoly.client.session.desktop.DesktopSessionRenderView}
 * to receive forwarded mouse click events from {@code MonopolyApp.mousePressed()}.</p>
 */
public interface MouseInteractiveView {
    void handleMousePressed(int mouseX, int mouseY);
}
