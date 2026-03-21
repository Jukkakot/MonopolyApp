package fi.monopoly.images;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(MonopolyRuntime runtime, Coordinates coords, SpotType spotType) {
        if (spotType.isProperty) {
            if (PlaceType.STREET.equals(spotType.streetType.placeType)) {
                return new StreetPropertySpotImage(runtime, coords, spotType);
            } else {
                return new PropertySpotImage(runtime, coords, spotType);
            }
        } else {
            return new SpotImage(runtime, coords, spotType);
        }
    }
}
