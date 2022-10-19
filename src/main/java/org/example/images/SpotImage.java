package org.example.images;

import lombok.Getter;
import org.example.components.Drawable;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;
import processing.core.PApplet;

public class SpotImage extends Image implements Drawable {
    @Getter
    protected SpotTypeEnum spotTypeEnum;

    public SpotImage(PApplet p, Coordinates coords) {
        this(p, coords, false);
    }

    public SpotImage(PApplet p, Coordinates coords, boolean isCorner) {
        super(p, coords);
        this.width = isCorner ? Spot.spotH : Spot.spotW;
        this.height = Spot.spotH;
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, true);
    }

    public void draw(Coordinates c, boolean pushPop) {
        if(c == null) {
            c = coords;
        }

        if (pushPop) {
            p.push();
        }
        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(c.x(), c.y());

        //Outside border
        p.rotate(MonopolyApp.radians(c.rotation()));
        p.rect(-width / 2, -height / 2, width, height);

        if (!spotTypeEnum.getProperty("price").trim().isEmpty()) {
            p.fill(0);
            p.textAlign(p.CENTER);
            p.textFont(MonopolyApp.font10);

            p.text("M" + spotTypeEnum.getProperty("price"), (int) -(width * 0.37), height / 3, (int) (width * 0.75), height);
        }

        if(pushPop) {
            p.pop();
        }
    }
}
