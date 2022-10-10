package org.example.images;

import org.example.MonopolyApp;
import org.example.spots.Drawable;
import processing.core.PApplet;

public class Image extends MonopolyApp implements Drawable {
    protected final int x;
    protected final int y;
    protected PApplet p;

    public Image(PApplet p, int x, int y) {
        this.x = x;
        this.y = y;
        this.p = p;
    }

    @Override
    public void draw(float rotate) {
        println("Default draw",x,y,rotate);
    }
    @Override
    public void draw() {
        draw(0);
    }
}
