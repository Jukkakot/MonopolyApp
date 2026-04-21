package fi.monopoly.client.desktop;

/**
 * Transitional holder for the current desktop Processing app instance.
 *
 * <p>This replaces the old {@code MonopolyApp.self} global with an explicit client-desktop context
 * seam so the app instance no longer masquerades as general-purpose global state.</p>
 */
public final class DesktopClientContext {
    private static MonopolyApp currentApp;

    private DesktopClientContext() {
    }

    public static void setCurrentApp(MonopolyApp app) {
        currentApp = app;
    }

    public static MonopolyApp currentApp() {
        if (currentApp == null) {
            throw new IllegalStateException("Desktop client app has not been initialized yet");
        }
        return currentApp;
    }
}
