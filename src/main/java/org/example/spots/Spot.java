package org.example.spots;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.Player;
import org.example.images.Image;
import org.example.images.PropertyImage;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import processing.core.PApplet;


import java.util.List;

public class Spot extends MonopolyApp implements Drawable {
    public static final int spotW = 1000/12;
    public static final int spotH = (int) (spotW * 1.5);
    @Getter
    protected final int x;
    @Getter
    protected final int y;
    @Getter
    protected final float rotation;
    private Image image;
    protected final PApplet p;

    public Spot(PApplet p, final int x, final int y, final float rotation) {
        this.p = p;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
    }

    @Override
    public void draw(float rotate) {
        if (image != null) {
            image.draw(rotate);
        } else {
            Image.defaultDraw(p, x, y);
        }

    }

    @Override
    public void draw() {
        draw(rotation);
    }

    public void setImage(SpotType spotType) {
        if (spotType.name().startsWith("CORNER")) {
            this.image = new SpotImage(p, this, true);
        } else if (spotType.streetType == null) {
            this.image = new SpotImage(p, this);
        } else {
            this.image = new PropertyImage(p, this, spotType);
        }
    }
}
