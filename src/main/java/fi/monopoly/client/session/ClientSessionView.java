package fi.monopoly.client.session;

import java.util.List;

/**
 * Minimal client-renderable session view surface.
 *
 * <p>This intentionally stays smaller than the current {@code Game} host. It is the first step
 * toward making the Processing client depend on a renderable session abstraction instead of the
 * concrete local runtime host implementation.</p>
 */
public interface ClientSessionView {
    void draw();

    List<String> debugPerformanceLines(float fps);
}
