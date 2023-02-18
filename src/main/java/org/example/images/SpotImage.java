package org.example.images;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.Drawable;
import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import org.example.utils.Utils;

import static org.example.utils.Utils.isMouseInArea;

public class SpotImage extends Image implements Drawable {
    @Getter
    protected SpotType spotType;
    protected boolean isHovered = false;

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
        if (c == null) {
            c = coords;
        }
        isHovered = isMouseInArea(new SpotProps(c, width, height));

        p.push();

        p.noFill();
        p.strokeWeight(isHovered ? 6 : 3);
        p.stroke(0);

        p.translate(c.x(), c.y());

        //Outside border
        p.rotate(MonopolyApp.radians(c.r()));
        p.rect(-width / 2, -height / 2, width, height);
        if (!spotType.getProperty("price").trim().isEmpty()) {
            drawPrice();
        }


        p.pop();

        if (MonopolyApp.DEBUG_MODE) {
            p.push();
            int[] rectCoords = Utils.getCoords(new SpotProps(c, width, height));
            p.fill(255, 0, 0, 30);
            p.noStroke();
            if (c.r() == 0 || c.r() == 180) {
                p.rect(rectCoords[0], rectCoords[3], width, height);
            } else {
                p.rect(rectCoords[0], rectCoords[3], height, width);
            }
//            p.stroke(255, 0, 0, 30);
//            p.line(rectCoords[0], rectCoords[1], rectCoords[2], rectCoords[3]);
//            p.line(rectCoords[0], rectCoords[3], rectCoords[2], rectCoords[1]);
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
