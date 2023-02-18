package org.example.components.event;

import processing.event.Event;

import java.util.EventListener;

public interface MonopolyEventListener extends EventListener {
    void onEvent(Event keyEvent);
}
