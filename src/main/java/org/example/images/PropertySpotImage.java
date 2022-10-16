package org.example.images;

import javafx.scene.paint.Color;
import org.example.Drawable;
import org.example.MonopolyApp;
import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;
import processing.core.PApplet;

import static org.example.utils.Utils.toColor;

public class PropertySpotImage extends SpotImage implements Drawable {
    public PropertySpotImage(PApplet p, Coordinates coords, SpotTypeEnum spotTypeEnum) {
        super(p, coords);
        this.spotTypeEnum = spotTypeEnum;
    }

    @Override
    public void draw(Coordinates c) {
        float rotation = c != null ? c.rotation() : 0;
        super.draw(c);
        p.push();
        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(coords.x(), coords.y());

        //Property color
        p.rotate(MonopolyApp.radians((coords.rotation() + rotation)));
        if (spotTypeEnum.streetType != null && spotTypeEnum.streetType.color != null) {
            Color color = spotTypeEnum.streetType.color;
            p.fill(toColor(p, color));
        }
        p.rect(-width / 2, -height / 2, width, height / 4);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotTypeEnum.getProperty("name"), (int) -(width * 0.37), -height / 6, (int) (width * 0.75), height / 2);

        p.pop();
    }
}
