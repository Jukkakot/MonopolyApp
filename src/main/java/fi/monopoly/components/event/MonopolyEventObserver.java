package fi.monopoly.components.event;

import fi.monopoly.client.desktop.DesktopClientSettings;
import lombok.extern.slf4j.Slf4j;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

@Slf4j
public abstract class MonopolyEventObserver extends PApplet {
    protected abstract MonopolyEventBus eventBusOrNull();

    @Override
    protected void dequeueEvents() {
        MonopolyEventBus eventBus = eventBusOrNull();
        if (eventBus != null) {
            eventBus.flushPendingChanges();
        }
        super.dequeueEvents();
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        MonopolyEventBus eventBus = eventBusOrNull();
        if (eventBus != null) {
            eventBus.sendConsumableEvent(keyEvent);
        }
        char key = Character.toLowerCase(keyEvent.getKey());
        if (key == 'd') {
            DesktopClientSettings.toggleDebugMode();
            log.debug("Debug mode {},", DesktopClientSettings.debugMode());
        }
        if (key == 'h') {
            log.info("""
                    
                    ----HELP-----
                    H = help
                    Ctrl+S = save local session
                    Ctrl+L = load local session
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
        MonopolyEventBus eventBus = eventBusOrNull();
        if (eventBus != null) {
            eventBus.sendEventToAll(mouseEvent);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        MonopolyEventBus eventBus = eventBusOrNull();
        if (eventBus != null) {
            eventBus.sendConsumableEvent(mouseEvent);
        }
    }

}
