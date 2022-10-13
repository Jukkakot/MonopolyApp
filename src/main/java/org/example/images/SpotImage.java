package org.example.images;

import org.example.spots.Spot;
import processing.core.PApplet;

public class SpotImage extends Image {
    protected final float width;
    protected final float height;

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
        super.draw(rotate);
        p.push();

        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(x, y);

        //Outside border
        p.rotate(radians(rotation + rotate));
        p.rect(-width/2, -height/2, width, height);

        p.pop();
    }
}
