package fi.monopoly.components.popup;


import controlP5.Canvas;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.event.MonopolyEventListener;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.KeyEvent;

import static processing.core.PConstants.CENTER;

@Slf4j
public abstract class Popup extends Canvas implements MonopolyEventListener {
    protected static final int DEFAULT_POPUP_WIDTH = 500;
    protected static final int DEFAULT_POPUP_HEIGHT = 300;
    protected static final int MIN_POPUP_WIDTH = 320;
    protected static final int MIN_POPUP_HEIGHT = 220;
    protected static final int WINDOW_MARGIN = 32;
    protected static final int TEXT_TOP_OFFSET = 24;
    protected static final int TEXT_HEIGHT = 140;
    protected static final int BUTTON_AREA_TOP_PADDING = 90;
    protected static final int BUTTON_AREA_BOTTOM_PADDING = 30;
    protected final MonopolyRuntime runtime;
    protected String popupText;
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
        Coordinates coords = getPopupCenter();
        float width = getPopupWidth();
        float height = getPopupHeight();
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
        p.text(popupText, -width / 2f + 20, -height / 2f + TEXT_TOP_OFFSET, width - 40, TEXT_HEIGHT);

        p.pop();
    }

    public void setPopupText(String text) {
        this.popupText = MonopolyUtils.parseIllegalCharacters(text);
    }

    public String getPopupText() {
        return popupText;
    }

    protected boolean onKeyAction(char key) {
        return false;
    }

    protected Coordinates getPopupCenter() {
        return Coordinates.of(runtime.app().width / 2f, runtime.app().height / 2f);
    }

    protected int getPopupWidth() {
        int availableWidth = runtime.app().width - WINDOW_MARGIN * 2;
        return Math.max(MIN_POPUP_WIDTH, Math.min(DEFAULT_POPUP_WIDTH, availableWidth));
    }

    protected int getPopupHeight() {
        int availableHeight = runtime.app().height - WINDOW_MARGIN * 2;
        return Math.max(MIN_POPUP_HEIGHT, Math.min(DEFAULT_POPUP_HEIGHT, availableHeight));
    }

    protected float getPopupLeft() {
        return getPopupCenter().x() - getPopupWidth() / 2f;
    }

    protected float getPopupTop() {
        return getPopupCenter().y() - getPopupHeight() / 2f;
    }

    protected float getPopupRight() {
        return getPopupLeft() + getPopupWidth();
    }

    protected float getPopupBottom() {
        return getPopupTop() + getPopupHeight();
    }

    protected float getButtonAreaTop() {
        return getPopupTop() + BUTTON_AREA_TOP_PADDING;
    }

    protected float getButtonAreaHeight() {
        return Math.max(0, getPopupHeight() - BUTTON_AREA_TOP_PADDING - BUTTON_AREA_BOTTOM_PADDING);
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
