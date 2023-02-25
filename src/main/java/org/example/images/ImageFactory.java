package org.example.images;

import org.example.components.spots.propertySpots.PropertySpot;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

public class ImageFactory {
    public static SpotImage getImage(Coordinates coords, SpotType st) {
        if (st.streetType == null) {
            return new SpotImage(coords);
        } else if (st.streetType.imgName != null) {
            return new IconSpotImage(coords, st);
        } else {
            return new StreetSpotImage(coords, st);
        }
    }

    public static SpotImage getImage(PropertySpot ps) {
        SpotImage img = ps.getImage();
        if (img instanceof IconSpotImage) {
            return new IconSpotImage(ps);
        } else if (img instanceof StreetSpotImage) {
            return new StreetSpotImage(ps);
        } else {
            return new SpotImage(ps);
        }
    }
}
