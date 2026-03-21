package fi.monopoly.components.event;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import lombok.extern.slf4j.Slf4j;
import processing.core.PApplet;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

@Slf4j
public class MonopolyEventObserver extends PApplet {
    private MonopolyEventBus eventBus() {
        return MonopolyRuntime.get().eventBus();
    }

    @Override
    protected void dequeueEvents() {
        eventBus().flushPendingChanges();
        super.dequeueEvents();
    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        super.keyPressed(keyEvent);
        eventBus().sendConsumableEvent(keyEvent);
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
        eventBus().sendEventToAll(mouseEvent);
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        super.mousePressed(mouseEvent);
        eventBus().sendConsumableEvent(mouseEvent);
    }

}
