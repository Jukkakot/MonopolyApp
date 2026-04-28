package fi.monopoly.client.desktop;

/**
 * Factory for wiring one desktop client host binding.
 *
 * <p>Embedded local mode is the current implementation, but the app shell should ask for a client
 * host binding rather than constructing one embedded-specific graph directly.</p>
 */
public interface DesktopClientHostBindingFactory {
    DesktopClientHostBinding create(
            MonopolyApp app,
            Runnable saveLocalSessionAction,
            Runnable loadLocalSessionAction
    );
}
