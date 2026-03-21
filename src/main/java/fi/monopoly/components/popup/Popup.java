package fi.monopoly.components.popup;


import controlP5.Canvas;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import fi.monopoly.components.event.MonopolyEventListener;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.KeyEvent;

import static processing.core.PConstants.CENTER;

@Slf4j
public abstract class Popup extends Canvas implements MonopolyEventListener {
    protected final MonopolyRuntime runtime;
    protected String popupText;
    protected static Coordinates coords = new Coordinates(Spot.SPOT_W * 6, Spot.SPOT_W * 6);
    protected static int width = 500;
    protected static int height = 300;
    @Getter(AccessLevel.PROTECTED)
    protected boolean isVisible = false;

    protected Popup(MonopolyRuntime runtime) {
        this.runtime = runtime;
        runtime.controlP5().addCanvas(this);
        runtime.eventBus().addListener(this);
    }

    protected void show() {
        isVisible = true;
        log.info(popupText);
    }

    protected void hide() {
        isVisible = false;
    }

    protected void allButtonAction() {
        hide();
        runtime.popupService().onPopupClosed(this);
    }

    protected final void completeAction(ButtonAction action) {
        allButtonAction();
        if (action != null) {
            action.doAction();
        }
        runtime.popupService().showNextPending();
    }

    @Override
    public void draw(PGraphics p) {
        if (!isVisible) {
            return;
        }
        p.push();

        p.translate(coords.x(), coords.y());

        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(0, 0, width, height, 30);

        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER);
        p.text(popupText, 5, -20, width, 200);

        p.pop();
    }

    public void setPopupText(String text) {
        this.popupText = MonopolyUtils.parseIllegalCharacters(text);
    }

    protected boolean onKeyAction(char key) {
        return false;
    }

    @Override
    public final boolean onEvent(Event event) {
        if (!isVisible) {
            return false;
        }
        if (event instanceof KeyEvent keyEvent) {
            return onKeyAction(keyEvent.getKey());
        }
        return false;
    }
}
