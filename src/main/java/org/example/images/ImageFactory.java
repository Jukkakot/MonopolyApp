package org.example.images;

import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(Coordinates coords, SpotType sp) {
        if (sp.name().startsWith("CORNER")) {
            return new IconSpotImage(coords, sp, true);
        } else if (sp.streetType == null) {
            return new SpotImage(coords);
        } else if (sp.streetType.imgName != null) {
            return new IconSpotImage(coords, sp);
        } else {
            return new StreetSpotImage(coords, sp);
        }
    }
}
