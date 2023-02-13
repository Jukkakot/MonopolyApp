package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.images.ImageFactory;
import org.example.images.SpotImage;
import org.example.types.PlaceType;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class SpotFactory {

    public static Spot getSpot(MonopolyApp p, Coordinates coords, SpotType sp) {
        PlaceType pt = sp.streetType.placeType;
        SpotImage image = ImageFactory.getImage(p, coords, sp);
        if (pt.equals(PlaceType.STREET)) {
            return new StreetPropertySpot(p, image);
        } else if (pt.equals(PlaceType.RAILROAD)) {
            return new RailRoadPropertySpot(p, image);
        } else if (pt.equals(PlaceType.UTILITY)) {
            return new UtilityPropertySpot(p, image);
        } else {
            return new Spot(p, image);
        }
    }
}
