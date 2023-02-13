package org.example.components.popup;


import controlP5.Canvas;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import processing.core.PGraphics;

import static processing.core.PConstants.CENTER;

public class Popup extends Canvas {
    @Setter
    protected String popupText;
    protected final MonopolyApp p;
    protected Coordinates coords = new Coordinates(Spot.spotW * 6, Spot.spotW * 6);
    protected int width = 500;
    protected int height = 300;
    protected boolean isVisible = false;

    public Popup(MonopolyApp p) {
        this.p = p;
        p.p5.addCanvas(this);
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
        p.textSize(20);
        p.textAlign(CENTER);
        p.text(popupText, 0, -20, width, 200);

        p.pop();
    }

    public static void showInfo(MonopolyApp p, String text) {
        showInfo(p, text, null);
    }

    public static void showInfo(MonopolyApp p, String text, ButtonAction onAccept) {
        OkPopup okPopup = new OkPopup(p);
        okPopup.setOnAccept(onAccept);
        okPopup.setPopupText(text);
        okPopup.show();
    }

    public static void showChoice(MonopolyApp p, String text, ButtonAction onAccept) {
        showChoice(p, text, onAccept, null);
    }

    public static void showChoice(MonopolyApp p, String text, ButtonAction onAccept, ButtonAction onDecline) {
        ChoicePopup choicePopup = new ChoicePopup(p);
        choicePopup.setPopupText(text);
        choicePopup.setOnAccept(onAccept);
        choicePopup.setOnDecline(onDecline);
        choicePopup.show();
    }
}
