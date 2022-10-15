package org.example.images;

import org.example.Drawable;
import org.example.components.Spot;
import org.example.types.SpotType;
import processing.core.PApplet;

public class SpotImage extends Image implements Drawable {
    protected SpotType spotType;
    public SpotImage(PApplet p, Spot spot) {
        this(p, spot, false);
    }
    public SpotImage(PApplet p, Spot spot, boolean isCorner) {
        super(p, spot);
        this.width = isCorner ? Spot.spotH : Spot.spotW;
        this.height = Spot.spotH;
    }

    @Override
    public void draw(float rotate) {
        p.push();

        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(x, y);

        //Outside border
        p.rotate(radians(rotation + rotate));
        p.rect(-width/2, -height/2, width, height);

        if(!spotType.getProperty("price").trim().isEmpty()) {
            p.fill(0);
            p.textAlign(CENTER);
            p.textFont(font);

            p.text("M"+spotType.getProperty("price"), (int) -(width * 0.37), height/3, (int) (width * 0.75), height);
        }

        p.pop();
    }
}
