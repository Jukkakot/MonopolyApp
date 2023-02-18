package org.example.components.popup;


import controlP5.Canvas;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.components.event.MonopolyEventListener;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import processing.core.PGraphics;
import processing.event.Event;

import static processing.core.PConstants.CENTER;

public abstract class Popup extends Canvas implements MonopolyEventListener {
    @Setter
    protected String popupText;
    protected static Coordinates coords = new Coordinates(Spot.spotW * 6, Spot.spotW * 6);
    protected static int width = 500;
    protected static int height = 300;
    protected boolean isVisible = false;

    public Popup() {
        MonopolyApp.p5.addCanvas(this);
        MonopolyApp.addListener(this);
    }

    public void show() {
        isVisible = true;
    }

    protected void allButtonAction() {
        isVisible = false;
    }

    public boolean isVisible() {
        return isVisible;
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

    public static void showInfo(String text) {
        showInfo(text, null);
    }

    public static void showInfo(String text, ButtonAction onAccept) {
        OkPopup okPopup = OkPopup.getInstance();
        okPopup.setOnOkAction(onAccept);
        okPopup.setPopupText(text);
        okPopup.show();
    }

    public static void showChoice(String text, ButtonAction onAccept) {
        showChoice(text, onAccept, null);
    }

    public static void showChoice(String text, ButtonAction onAccept, ButtonAction onDecline) {
        ChoicePopup choicePopup = ChoicePopup.getInstance();
        choicePopup.setPopupText(text);
        choicePopup.setOnAcceptAction(onAccept);
        choicePopup.setOnDeclineAction(onDecline);
        choicePopup.show();
    }

    @Override
    public void onEvent(Event event) {}
}
