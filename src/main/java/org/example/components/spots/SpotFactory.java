package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.images.ImageFactory;
import org.example.types.PlaceType;
import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;

public class SpotFactory {

    public static Spot getSpot(MonopolyApp p, Coordinates coords, SpotTypeEnum sp) {
        PlaceType pt = sp.streetType.placeType;
        if (pt.equals(PlaceType.STREET)) {
            return new StreetPropertySpot(p, ImageFactory.getImage(p, coords, sp));
        } else if (pt.equals(PlaceType.RAILROAD) || pt.equals(PlaceType.UTILITY)) {
            return new PropertySpot(p, ImageFactory.getImage(p, coords, sp));
        } else {
            return new Spot(p, ImageFactory.getImage(p, coords, sp));
        }
    }
}
