package fi.monopoly.components.event;

import fi.monopoly.MonopolyApp;
import lombok.extern.slf4j.Slf4j;
import processing.core.PApplet;
import processing.event.Event;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class MonopolyEventObserver extends PApplet {
    private static final Set<MonopolyEventListener> eventListeners = new HashSet<>();
    private static final List<MonopolyEventListener> addListeners = new ArrayList<>();
    private static final List<MonopolyEventListener> removeListeners = new ArrayList<>();

    @Override
    protected void dequeueEvents() {
        if (!addListeners.isEmpty()) {
            eventListeners.addAll(addListeners);
            addListeners.clear();
        }
        if (!removeListeners.isEmpty()) {
            removeListeners.forEach(eventListeners::remove);
            removeListeners.clear();
        }
        super.dequeueEvents();
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        sendConsumableEvent(keyEvent);
        if (keyEvent.getKey() == 'd') {
            MonopolyApp.DEBUG_MODE = !MonopolyApp.DEBUG_MODE;
            log.debug("Debug mode {},", MonopolyApp.DEBUG_MODE);
        }
        if (keyEvent.getKey() == 'h') {
            log.info("""
                                        
                    ----HELP-----
                    H = help
                    E = end round
                    A = skip animation
                    D = debug mode
                    ----HELP-----
                    """);
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
        eventListeners.stream().anyMatch(listener -> listener.onEvent(event));
    }

    private void sendEventToALl(Event event) {
        eventListeners.forEach(eventListener -> eventListener.onEvent(event));
    }

    public static void addListener(MonopolyEventListener listener) {
        addListeners.add(listener);
    }

    public static void removeListener(MonopolyEventListener listener) {
        removeListeners.add(listener);
    }
}
