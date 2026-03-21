package fi.monopoly.images;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;

public class PropertySpotImage extends SpotImage {
    private final Property property;

    public PropertySpotImage(MonopolyRuntime runtime, Coordinates coords, SpotType spotType) {
        super(runtime, coords, spotType);
        this.property = PropertyFactory.getProperty(spotType);
    }

    @Override
    public void draw(Coordinates c) {
        super.draw(c);
        if (property.isMortgaged()) {
            drawMortgaged();
        }
    }

    protected void drawMortgaged() {
        MonopolyApp p = runtime.app();
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
}
