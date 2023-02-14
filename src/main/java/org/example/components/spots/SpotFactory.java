package org.example.components.spots;

import org.example.images.ImageFactory;
import org.example.images.SpotImage;
import org.example.types.PlaceType;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class SpotFactory {

    public static Spot getSpot(Coordinates coords, SpotType sp) {
        PlaceType pt = sp.streetType.placeType;
        SpotImage image = ImageFactory.getImage(coords, sp);
        if (pt.equals(PlaceType.STREET)) {
            return new StreetPropertySpot(image);
        } else if (pt.equals(PlaceType.RAILROAD)) {
            return new RailRoadPropertySpot(image);
        } else if (pt.equals(PlaceType.UTILITY)) {
            return new UtilityPropertySpot(image);
        } else {
            return new Spot(image);
        }
    }
}
