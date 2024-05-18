package org.example.images;

import javafx.scene.paint.Color;
import org.example.MonopolyApp;
import org.example.components.properties.Property;
import org.example.components.properties.PropertyFactory;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import static org.example.utils.Utils.toColor;

public class PropertySpotImage extends SpotImage {
    private final Property property;

    public PropertySpotImage(Coordinates coords, SpotType spotType) {
        super(coords, spotType);
        this.property = PropertyFactory.getProperty(spotType);
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        if(isStreetSpot())  {
            drawStreetSpot();
        }
        if (property.isMortgaged()) {
            drawMortgaged();
        }
    }

    private boolean isStreetSpot() {
        return spotType.streetType != null && spotType.streetType.color != null;
    }

    private void drawMortgaged() {
        p.push();

        p.strokeWeight(3);
        p.stroke(255, 0, 0);

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians(coords.r()));

        //Draw x
        p.line(-getWidth() / 2, -getHeight() / 2, getWidth() / 2, getHeight() / 2);
        p.line(-getWidth() / 2, getHeight() / 2, getWidth() / 2, -getHeight() / 2);

        p.pop();
    }
    private void drawStreetSpot() {
        p.push();

        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(coords.x(), coords.y());

        //Property color
        p.rotate(MonopolyApp.radians((coords.r())));
        if (spotType.streetType != null && spotType.streetType.color != null) {
            Color color = spotType.streetType.color;
            p.fill(toColor(color));
        }
        p.rect(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight() / 4);

        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(MonopolyApp.font10);
        p.textLeading(10);
        p.text(spotType.getStringProperty("name"), (int) -(getWidth() * 0.37), -getHeight() / 6, (int) (getWidth() * 0.75), getHeight() / 2);

        p.pop();
    }
}
