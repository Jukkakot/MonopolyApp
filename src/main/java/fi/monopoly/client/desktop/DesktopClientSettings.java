package fi.monopoly.client.desktop;

/**
 * Global desktop-client feature flags and transient client-side runtime settings.
 *
 * <p>These values still live as process-wide state for the current single-process desktop client,
 * but they no longer belong to {@link MonopolyApp}. This keeps the Processing app focused on app
 * lifecycle while making the remaining client-global state explicit.</p>
 */
public final class DesktopClientSettings {
    private static boolean debugMode;
    private static boolean skipAnimations;

    private DesktopClientSettings() {
    }

    public static boolean debugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean debugMode) {
        DesktopClientSettings.debugMode = debugMode;
    }

    public static void toggleDebugMode() {
        debugMode = !debugMode;
    }

    public static boolean skipAnimations() {
        return skipAnimations;
    }

    public static void setSkipAnimations(boolean skipAnimations) {
        DesktopClientSettings.skipAnimations = skipAnimations;
    }

    public static void toggleSkipAnimations() {
        skipAnimations = !skipAnimations;
    }
}
