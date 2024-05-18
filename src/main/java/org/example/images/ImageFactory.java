package org.example.images;

import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(Coordinates coords, SpotType spotType) {
        if (spotType.isProperty) {
            return new PropertySpotImage(coords, spotType);
        } else {
            return new SpotImage(coords, spotType);
        }
    }
}
