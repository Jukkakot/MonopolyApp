package org.example.images;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import org.example.utils.Utils;

import static org.example.utils.Utils.isMouseInArea;

public class SpotImage extends Image {
    @Getter
    protected SpotType spotType;

    public SpotImage(Coordinates coords) {
        this(coords, false);
    }

    public SpotImage(Coordinates coords, boolean isCorner) {
        super(coords);
        this.width = isCorner ? Spot.SPOT_H : Spot.SPOT_W;
        this.height = Spot.SPOT_H;
    }

    public SpotImage(PropertySpot ps) {
        super(new SpotProps(ps.getCoords(), Spot.SPOT_W, Spot.SPOT_H), ps.getSpotType().streetType.imgName);
        this.spotType = ps.getSpotType();
    }

    @Override
    public void draw(Coordinates c) {
        if (c == null) {
            c = coords;
        }
        updateIsHovered(c);

        p.push();

        p.fill(205, 230, 209);
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(c.x(), c.y());

        //Outside border
        p.rotate(MonopolyApp.radians(c.r()));
        p.rect(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight());
        if (!spotType.getProperty("price").trim().isEmpty()) {
            drawPrice();
        }

        p.pop();

        if (MonopolyApp.DEBUG_MODE) {
            p.push();
            int[] rectCoords = Utils.getCoords(new SpotProps(c, getWidth(), getHeight()));
            if (isHovered) {
                p.fill(0, 255, 0, 30);
            } else {
                p.fill(255, 0, 0, 30);
            }
            p.noStroke();
            if (c.r() == 0 || c.r() == 180) {
                p.rect(rectCoords[0], rectCoords[3], getWidth(), getHeight());
            } else {
                p.rect(rectCoords[0], rectCoords[3], getHeight(), getWidth());
            }
            p.pop();
        }
    }

    private void updateIsHovered(Coordinates c) {
        isHovered = isMouseInArea(new SpotProps(c, getWidth(), getHeight()));
    }

    private void drawPrice() {
        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.text("M" + spotType.getProperty("price"), (int) -(getWidth() * 0.37), getHeight() / 3, (int) (getWidth() * 0.75), getHeight());
    }
}
