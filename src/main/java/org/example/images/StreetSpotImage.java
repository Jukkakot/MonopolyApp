package org.example.images;

import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import static org.example.utils.Utils.toColor;

public class StreetSpotImage extends SpotImage {
    public StreetSpotImage(Coordinates coords, SpotType spotType) {
        super(coords);
        this.spotType = spotType;
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        if (c == null) {
            c = coords;
        }
        p.push();

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
        p.rect(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight() / 4);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotType.getProperty("name"), (int) -(getWidth() * 0.37), -getHeight() / 6, (int) (getWidth() * 0.75), getHeight() / 2);

        p.pop();
    }
}
