package org.example.components.spots;

import lombok.experimental.UtilityClass;
import org.example.images.ImageFactory;
import org.example.images.SpotImage;
import org.example.types.PlaceType;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

@UtilityClass
public class SpotFactory {

    public Spot getSpot(Coordinates coords, SpotType spotType) {
        PlaceType pt = spotType.streetType.placeType;
        SpotImage image = ImageFactory.getImage(coords, spotType);
        if (spotType.isProperty) {
            return new PropertySpot(image, spotType);
        } else if (pt.equals(PlaceType.TAX)) {
            return new TaxSpot(image);
        } else if (pt.equals(PlaceType.PICK_CARD)) {
            return new PickCardSpot(image);
        } else if (spotType.equals(SpotType.GO_TO_JAIL)) {
            return new GoToJailSpot(image);
        } else if (spotType.equals(SpotType.JAIL)) {
            return new JailSpot(image);
        } else {
            return new CornerSpot(image);
        }
    }
}
