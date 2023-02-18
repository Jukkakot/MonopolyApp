package org.example.components.event;

import processing.event.Event;

import java.util.EventListener;

public interface MonopolyEventListener extends EventListener {
    /**
     * Listens to mouse and keyboard events
     *
     * @param event event that has happened
     * @return true if consumed this event
     */
    boolean onEvent(Event event);
}
