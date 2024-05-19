package org.example.components.popup;


import controlP5.Canvas;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.MonopolyApp;
import org.example.components.event.MonopolyEventListener;
import org.example.components.popup.components.ButtonProps;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import processing.core.PGraphics;
import processing.event.Event;
import processing.event.KeyEvent;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static processing.core.PConstants.CENTER;

@Slf4j
public abstract class Popup extends Canvas implements MonopolyEventListener {
    protected static final Map<Class<? extends Popup>, Popup> popupInstances = new HashMap<>();
    @Setter
    protected String popupText;
    protected static Coordinates coords = new Coordinates(Spot.SPOT_W * 6, Spot.SPOT_W * 6);
    protected static int width = 500;
    protected static int height = 300;
    @Getter(AccessLevel.PROTECTED)
    protected boolean isVisible = false;

    protected Popup() {
        MonopolyApp.p5.addCanvas(this);
    }

    private static <T extends Popup> T getInstance(Class<T> clazz) {
        if (popupInstances.get(clazz) == null) {
            try {
                popupInstances.put(clazz, clazz.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return (T) popupInstances.get(clazz);
    }

    protected void show() {
        isVisible = true;
        MonopolyApp.addListener(this);
        log.info(popupText);
    }

    public static void hideAll() {
        popupInstances.values().forEach(Popup::hide);
    }

    protected void hide() {
        isVisible = false;
        MonopolyApp.removeListener(this);
    }

    protected void allButtonAction() {
        isVisible = false;
        hide();
    }

    public static boolean isAnyVisible() {
        return popupInstances.values().stream().anyMatch(Popup::isVisible);
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
        p.textFont(MonopolyApp.font20);
        p.textAlign(CENTER);
        p.text(popupText, 5, -20, width, 200);

        p.pop();
    }

    public static void show(String text) {
        OkPopup okPopup = Popup.getInstance(OkPopup.class);
        okPopup.setOnOkAction(null);
        okPopup.setPopupText(text);
        okPopup.show();
    }

    public static void show(String text, ButtonAction onAccept) {
        OkPopup okPopup = Popup.getInstance(OkPopup.class);
        okPopup.setOnOkAction(onAccept);
        okPopup.setPopupText(text);
        okPopup.show();
    }


    public static void show(String text, ButtonAction onAccept, ButtonAction onDecline) {
        ChoicePopup choicePopup = Popup.getInstance(ChoicePopup.class);
        choicePopup.setPopupText(text);
        choicePopup.setOnAcceptAction(onAccept);
        choicePopup.setOnDeclineAction(onDecline);
        choicePopup.show();
    }

    public static void show(String text, ButtonProps... buttonProps) {
        CustomPopup customPopup = Popup.getInstance(CustomPopup.class);
        customPopup.setPopupText(text);
        customPopup.setButtons(buttonProps);
        customPopup.show();
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
