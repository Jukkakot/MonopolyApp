package org.example.images;

import javafx.scene.paint.Color;
import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotType;
import processing.core.PApplet;
import static org.example.Utils.toFloat;

public class PropertyImage extends SpotImage implements Drawable {

    private final SpotType spotType;

    public PropertyImage(PApplet p, Spot spot, SpotType spotType) {
        super(p, spot);
        this.spotType = spotType;
    }
    @Override
    public void draw() {
        draw(0);
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
        p.rotate(radians((rotation + rotate)));
        if (spotType.streetType != null && spotType.streetType.color != null) {
            Color color = spotType.streetType.color;
            p.fill(toFloat(color.getRed()), toFloat(color.getGreen()), toFloat(color.getBlue()));
        }
        p.rect(-width / 2, -height / 2, width, height / 4);

        p.pop();
    }
}
