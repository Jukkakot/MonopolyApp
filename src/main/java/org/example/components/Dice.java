package org.example.components;

import org.example.images.Image;
import org.example.Drawable;
import org.example.types.SpotProps;
import org.example.utils.Coordinates;
import processing.core.PApplet;

public class Dice implements Drawable {
    private int value = 1;
    private final Image img;

    public Dice(PApplet p, SpotProps sp) {
        img = new Image(p, sp, getImgName());
    }

    public int roll() {
        value = (int) (Math.random() * 6) + 1;
        img.setImgName(getImgName());
        return value;
    }

    private String getImgName() {
        return "Dice" + value + ".png";
    }

    @Override
    public void draw(float rotate) {
        img.draw(rotate);
    }

    @Override
    public void draw() {
        draw(0);
    }

    @Override
    public void draw(Coordinates coordinates) {

    }

    @Override
    public Coordinates getCoords() {
        return img.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        img.setCoords(coords);
    }
}
