package org.example.images;

import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(Coordinates coords, SpotType st) {
        if (st.streetType == null) {
            return new SpotImage(coords, st);
        } else if (st.streetType.imgName != null) {
            return new IconSpotImage(coords, st);
        } else {
            return new StreetSpotImage(coords, st);
        }
    }
}
