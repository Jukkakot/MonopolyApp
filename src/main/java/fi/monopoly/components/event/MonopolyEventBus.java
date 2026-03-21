package fi.monopoly.components.event;

import processing.event.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonopolyEventBus {
    private final Set<MonopolyEventListener> eventListeners = new HashSet<>();
    private final List<MonopolyEventListener> addListeners = new ArrayList<>();
    private final List<MonopolyEventListener> removeListeners = new ArrayList<>();

    public void flushPendingChanges() {
        if (!addListeners.isEmpty()) {
            eventListeners.addAll(addListeners);
            addListeners.clear();
        }
        if (!removeListeners.isEmpty()) {
            removeListeners.forEach(eventListeners::remove);
            removeListeners.clear();
        }
    }

    public void addListener(MonopolyEventListener listener) {
        addListeners.add(listener);
    }

    public void removeListener(MonopolyEventListener listener) {
        removeListeners.add(listener);
    }

    public void sendConsumableEvent(Event event) {
        eventListeners.stream().anyMatch(listener -> listener.onEvent(event));
    }

    public void sendEventToAll(Event event) {
        eventListeners.forEach(eventListener -> eventListener.onEvent(event));
    }
}
