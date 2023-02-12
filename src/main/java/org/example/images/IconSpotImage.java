package org.example.images;

import org.example.components.Drawable;
import org.example.MonopolyApp;
import org.example.types.SpotTypeEnum;
import org.example.types.StreetType;
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
       draw(c, true);
    }
    @Override
    public void draw(Coordinates c, boolean pushPop) {
        if(c == null) {
            c = coords;
        }
        super.draw(c,pushPop);
        if(pushPop) {
            p.push();
        }
        p.translate(c.x(), c.y());
        p.rotate(MonopolyApp.radians((c.r())));
        p.imageMode(p.CENTER);
        String imgName = spotTypeEnum.streetType.imgName;
        if(spotTypeEnum.streetType.equals(StreetType.UTILITY)) {
            imgName = "Utility"+spotTypeEnum.id+".png";
        }
        PImage img = MonopolyApp.IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotTypeEnum.getProperty("name"), (int) -(width * 0.37), (int) -(height * 0.42), (int) (width * 0.75), height / 2);

        if(pushPop) {
            p.pop();
        }
    }
}
