package org.example.components.event;

import org.example.MonopolyApp;
import processing.core.PApplet;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.HashSet;
import java.util.Set;

public class EventObserver extends PApplet {
    private static final Set<MonopolyEventListener> eventListeners = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        sendConsumableEvent(keyEvent);
        if(keyEvent.getKey() == 'd') {
            MonopolyApp.DEBUG_MODE = !MonopolyApp.DEBUG_MODE;
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        super.mouseClicked(mouseEvent);
        sendEventToALl(mouseEvent);
    }

//    @Override
//    public void mouseMoved(MouseEvent mouseEvent) {
//        super.mouseMoved(mouseEvent);
//        sendEventToALl(mouseEvent);
////        sendConsumableEvent(mouseEvent);
//    }
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        sendConsumableEvent(mouseEvent);
    }

    private void sendConsumableEvent(Event event) {
        eventListeners.stream().anyMatch(listener -> listener.onEvent(event));
    }
    private void sendEventToALl(Event event) {
        eventListeners.forEach(eventListener -> eventListener.onEvent(event));
    }

    public static void addListener(MonopolyEventListener listener) {
        eventListeners.add(listener);
    }
}
