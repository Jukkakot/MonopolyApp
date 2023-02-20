package org.example.images;

import org.example.MonopolyApp;
import org.example.types.SpotType;
import org.example.utils.Coordinates;
import processing.core.PImage;

public class IconSpotImage extends SpotImage {

    public IconSpotImage(Coordinates coords, SpotType spotType) {
        this(coords, spotType, false);
    }

    public IconSpotImage(Coordinates coords, SpotType spotType, boolean isCorner) {
        super(coords, isCorner);
        this.spotType = spotType;
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        if (c == null) {
            c = coords;
        }
        p.push();

        p.translate(c.x(), c.y());
        p.rotate(MonopolyApp.radians((c.r())));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(spotType);
        p.image(img, 0, 0, getWidth(), getHeight());

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotType.getProperty("name"), (int) -(getWidth() * 0.37), (int) -(getHeight() * 0.42), (int) (getWidth() * 0.75), getHeight() / 2);

        p.pop();
    }
}
