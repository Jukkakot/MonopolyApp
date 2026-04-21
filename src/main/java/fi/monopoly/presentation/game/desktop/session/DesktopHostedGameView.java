package fi.monopoly.presentation.game.desktop.session;

import java.util.List;

/**
 * Client-facing render view for a hosted desktop game session.
 *
 * <p>This is intentionally narrower than the full hosted game lifecycle. The client only needs to
 * render the already-hosted local session and inspect debug overlay lines; frame advancement and
 * session ownership remain on the host side.</p>
 */
public interface DesktopHostedGameView {
    void draw();

    List<String> debugPerformanceLines(float fps);
}
