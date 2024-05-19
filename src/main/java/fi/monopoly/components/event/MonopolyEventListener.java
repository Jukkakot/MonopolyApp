package fi.monopoly.components.event;

import processing.event.Event;

public interface MonopolyEventListener {
    /**
     * Listens to mouse and keyboard events
     *
     * @param event event that has happened
     * @return true if consumed this event
     */
    boolean onEvent(Event event);
}
