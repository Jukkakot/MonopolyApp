package fi.monopoly.client.desktop;

import fi.monopoly.components.event.MonopolyEventBus;

/**
 * Client-side runtime access port for the active desktop runtime instance.
 *
 * <p>The app shell should depend on a narrow runtime access surface instead of the concrete
 * embedded bootstrap bridge. That keeps local embedded bootstrapping replaceable when a remote
 * client transport eventually owns session connectivity.</p>
 */
public interface DesktopRuntimeAccess {
    MonopolyRuntime runtimeOrNull();

    MonopolyRuntime runtime();

    default MonopolyEventBus eventBusOrNull() {
        MonopolyRuntime runtime = runtimeOrNull();
        return runtime != null ? runtime.eventBus() : null;
    }

    default MonopolyEventBus eventBus() {
        return runtime().eventBus();
    }
}
