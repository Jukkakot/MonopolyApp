package org.example.images;

import org.example.MonopolyApp;
import org.example.spots.Drawable;
import org.example.spots.Spot;
import processing.core.PApplet;

public class Image extends MonopolyApp implements Drawable {
    protected int x;
    protected int y;
    protected float rotation;
    protected PApplet p;

    public Image(PApplet p, Spot spot) {
        this.p = p;
        this.x = spot.getX();
        this.y = spot.getY();
        this.rotation = spot.getRotation();
    }

    @Override
    public void draw(float rotate) {
//        defaultDraw(p, x, y);
    }

    @Override
    public void draw() {
        draw(0);
    }

    public static void defaultDraw(PApplet p, int x, int y) {
        p.push();

        p.stroke(0);
        p.strokeWeight(10);
        p.point(x, y);

        p.pop();
    }
}
