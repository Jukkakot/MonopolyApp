package org.example.components.popup;


import controlP5.Canvas;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import processing.core.PGraphics;

import static processing.core.PConstants.CENTER;

public class Popup extends Canvas {

    protected final String popupText;
    protected final MonopolyApp p;
    protected Coordinates coords = new Coordinates(Spot.spotW * 6, Spot.spotW * 6);
    protected int width = 500;
    protected int height = 300;
    protected boolean isVisible = false;

    public Popup(MonopolyApp p, String popupText) {
        this.popupText = popupText;
        this.p = p;
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
}
