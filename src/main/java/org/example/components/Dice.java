package org.example.components;

import org.example.Drawable;
import org.example.images.Image;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;

public class Dice implements Drawable {
    private int value = 1;
    private final Image img;

    public Dice(SpotProps sp) {
        img = new Image(sp, getImgName());
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
    public void draw() {
        draw(null);
    }

    @Override
    public void draw(Coordinates c) {
        img.draw(c);
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
