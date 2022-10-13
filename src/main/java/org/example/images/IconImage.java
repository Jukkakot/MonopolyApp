package org.example.images;

import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotType;
import processing.core.PApplet;
import processing.core.PImage;

public class IconImage extends SpotImage implements Drawable {
    private final SpotType spotType;

    public IconImage(PApplet p, Spot spot, SpotType spotType) {
       this(p, spot, spotType, false);
    }
    public IconImage(PApplet p, Spot spot, SpotType spotType, boolean isCorner) {
        super(p, spot, isCorner);
        this.spotType = spotType;
    }
    @Override
    public void draw(float rotate) {
        super.draw(rotate);
        p.push();
        p.translate(x, y);
        p.rotate(radians((rotation + rotate)));
        p.imageMode(CENTER);
        PImage img = IMAGES.get(spotType.streetType.imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.fill(0);
        p.textAlign(CENTER);
        p.textFont(font);
        p.text(spotType.getName(), (int) -(width * 0.37), (int) -(height * 0.42), (int) (width * 0.75), height / 2);

        p.pop();
    }
}
