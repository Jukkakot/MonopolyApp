package org.example.images;

import javafx.scene.paint.Color;
import org.example.Drawable;
import org.example.MonopolyApp;
import org.example.components.Spot;
import org.example.types.SpotType;
import processing.core.PApplet;

import static org.example.utils.Utils.toColor;

public class PropertySpotImage extends SpotImage implements Drawable {
    public PropertySpotImage(PApplet p, Spot spot, SpotType spotType) {
        super(p, spot);
        this.spotType = spotType;
    }

    @Override
    public void draw(float rotate) {
        super.draw(rotate);
        p.push();
        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(x, y);

        //Property color
        p.rotate(MonopolyApp.radians((rotation + rotate)));
        if (spotType.streetType != null && spotType.streetType.color != null) {
            Color color = spotType.streetType.color;
            p.fill(toColor(p, color));
        }
        p.rect(-width / 2, -height / 2, width, height / 4);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font);
        p.textLeading(10);
        p.text(spotType.getProperty("name"), (int) -(width * 0.37), -height / 6, (int) (width * 0.75), height / 2);

        p.pop();
    }
}
