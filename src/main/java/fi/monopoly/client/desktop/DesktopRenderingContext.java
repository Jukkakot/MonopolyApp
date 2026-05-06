package fi.monopoly.client.desktop;

/**
 * Minimal desktop rendering-facing context used by utility and presentation helpers.
 *
 * <p>This narrows common rendering helpers away from the full {@link MonopolyApp} type so the
 * desktop client can keep shedding Processing-app coupling a seam at a time.
 */
public interface DesktopRenderingContext {
    int color(float red, float green, float blue);

    int mouseX();

    int mouseY();
}
