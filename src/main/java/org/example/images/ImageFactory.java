package org.example.images;

import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;
import processing.core.PApplet;

public class ImageFactory {
    public static SpotImage getImage(PApplet p, Coordinates coords, SpotTypeEnum sp) {
        if (sp.name().startsWith("CORNER")) {
            return new IconSpotImage(p, coords, sp, true);
        } else if (sp.streetType == null) {
            return new SpotImage(p, coords);
        } else if (sp.streetType.imgName != null) {
            return new IconSpotImage(p, coords, sp);
        } else {
            return new PropertySpotImage(p, coords, sp);
        }
    }
}
