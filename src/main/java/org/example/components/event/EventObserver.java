package org.example.components.event;

import org.example.MonopolyApp;
import processing.core.PApplet;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

public class EventObserver extends PApplet {
    private static final Set<MonopolyEventListener> eventListeners = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        sendConsumableEvent(keyEvent);
        if (keyEvent.getKey() == 'd') {
            MonopolyApp.DEBUG_MODE = !MonopolyApp.DEBUG_MODE;
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        super.mouseClicked(mouseEvent);
        sendEventToALl(mouseEvent);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        sendConsumableEvent(mouseEvent);
    }

    private void sendConsumableEvent(Event event) {
        try {
            eventListeners.stream().anyMatch(listener -> listener.onEvent(event));
        } catch (ConcurrentModificationException e) {
            //Ignore for now..
            System.err.println("Concurrent error... " + e);
        }
    }

    private void sendEventToALl(Event event) {
        try {
            eventListeners.forEach(eventListener -> eventListener.onEvent(event));
        } catch (ConcurrentModificationException e) {
            //Ignore for now..
            System.err.println("Concurrent error... " + e);
        }
    }

    public static void addListener(MonopolyEventListener listener) {
        eventListeners.add(listener);
    }
}
