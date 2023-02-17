package org.example.images;

import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.Drawable;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import static org.example.utils.Utils.toColor;

public class StreetSpotImage extends SpotImage implements Drawable {
    public StreetSpotImage(Coordinates coords, SpotType spotType) {
        super(coords);
        this.spotType = spotType;
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, true);
    }

    @Override
    public void draw(Coordinates c, boolean pushPop) {
        if (c == null) {
            c = coords;
        }
        super.draw(c, pushPop);
        if (pushPop) {
            p.push();
        }
        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(c.x(), c.y());

        //Property color
        p.rotate(MonopolyApp.radians((c.r())));
        if (spotType.streetType != null && spotType.streetType.color != null) {
            Color color = spotType.streetType.color;
            p.fill(toColor(color));
        }
        p.rect(-width / 2, -height / 2, width, height / 4);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotType.getProperty("name"), (int) -(width * 0.37), -height / 6, (int) (width * 0.75), height / 2);

        if (pushPop) {
            p.pop();
        }
    }
}
