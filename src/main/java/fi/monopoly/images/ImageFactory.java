package fi.monopoly.images;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(Coordinates coords, SpotType spotType) {
        if (spotType.isProperty) {
           if(PlaceType.STREET.equals(spotType.streetType.placeType)) {
               return new StreetPropertySpotImage(coords, spotType);
           } else {
               return new PropertySpotImage(coords, spotType);
           }
        } else {
            return new SpotImage(coords, spotType);
        }
    }
}
