package org.example.images;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.Drawable;
import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class SpotImage extends Image implements Drawable {
    @Getter
    protected SpotType spotType;

    public SpotImage(Coordinates coords) {
        this(coords, false);
    }

    public SpotImage(Coordinates coords, boolean isCorner) {
        super(coords);
        this.width = isCorner ? Spot.spotH : Spot.spotW;
        this.height = Spot.spotH;
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, true);
    }

    public void draw(Coordinates c, boolean pushPop) {
        if (c == null) {
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
        p.rotate(MonopolyApp.radians(c.r()));
        p.rect(-width / 2, -height / 2, width, height);

        if (!spotType.getProperty("price").trim().isEmpty()) {
            drawPrice();
        }

        if (pushPop) {
            p.pop();
        }
    }

    private void drawPrice() {
        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);

        p.text("M" + spotType.getProperty("price"), (int) -(width * 0.37), height / 3, (int) (width * 0.75), height);
    }
}
