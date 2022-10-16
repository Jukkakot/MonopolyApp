package org.example.images;

import org.example.Drawable;
import org.example.MonopolyApp;
import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;
import processing.core.PApplet;
import processing.core.PImage;

public class IconSpotImage extends SpotImage implements Drawable {

    public IconSpotImage(PApplet p, Coordinates coords, SpotTypeEnum spotTypeEnum) {
       this(p, coords, spotTypeEnum, false);
    }
    public IconSpotImage(PApplet p, Coordinates coords, SpotTypeEnum spotTypeEnum, boolean isCorner) {
        super(p, coords, isCorner);
        this.spotTypeEnum = spotTypeEnum;
    }
    @Override
    public void draw(Coordinates c) {
        float rotation = c != null ? c.rotation() : 0;
        super.draw(c);
        p.push();
        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians((coords.rotation() + rotation)));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get(spotTypeEnum.streetType.imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotTypeEnum.getProperty("name"), (int) -(width * 0.37), (int) -(height * 0.42), (int) (width * 0.75), height / 2);

        p.pop();
    }
}
