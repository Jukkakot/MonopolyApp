package fi.monopoly.images;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import javafx.scene.paint.Color;

import static processing.core.PConstants.CORNER;

public class StreetPropertySpotImage extends PropertySpotImage {
    private final StreetProperty streetProperty;

    public StreetPropertySpotImage(MonopolyRuntime runtime, Coordinates coords, SpotType spotType) {
        super(runtime, coords, spotType);
        this.streetProperty = (StreetProperty) PropertyFactory.getProperty(spotType);
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        drawStreetSpot(c);
        if (streetProperty.isMortgaged()) {
            drawMortgaged();
        }
    }

    private void drawStreetSpot(Coordinates c) {
        if (c != null) {
            coords = c;
        }
        MonopolyApp p = runtime.app();
        p.push();

        p.noFill();
        p.strokeWeight(3);
        p.stroke(0);

        p.translate(coords.x(), coords.y());

        //Property color
        p.rotate(MonopolyApp.radians((coords.r())));
        Color color = spotType.streetType.color;
        p.fill(MonopolyUtils.toColor(p, color));

        p.rect(-getWidth() / 2, -getHeight() / 2, getWidth(), getHeight() / 4);


        p.fill(0);
        p.textAlign(p.CENTER);
        p.textFont(runtime.font10());
        p.textLeading(10);
        p.text(spotType.getStringProperty("name"), (int) -(getWidth() * 0.37), -getHeight() / 6, (int) (getWidth() * 0.75), getHeight() / 2);

        p.translate(-getWidth() / 2, -getHeight() / 2);

        p.ellipseMode(CORNER);
        int houseWidth = (int) (getWidth() / 4);
        for (int i = 0; i < streetProperty.getHouseCount(); i++) {
            p.fill(0, 255, 0);
            p.circle(i * houseWidth, getHeight() / 16, houseWidth);
        }
        for (int i = 0; i < streetProperty.getHotelCount(); i++) {
            p.fill(255, 0, 0);
            p.circle(i * houseWidth, getHeight() / 16, houseWidth);
        }
        p.pop();
    }
}
