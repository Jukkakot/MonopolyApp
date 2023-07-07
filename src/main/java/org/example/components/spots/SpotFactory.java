package org.example.components.spots;

import lombok.experimental.UtilityClass;
import org.example.images.ImageFactory;
import org.example.images.SpotImage;
import org.example.types.PlaceType;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class SpotFactory {
    private final List<PlaceType> PROPERTY_PLACE_TYPES = Arrays.asList(PlaceType.STREET, PlaceType.RAILROAD, PlaceType.UTILITY);

    public Spot getSpot(Coordinates coords, SpotType spotType) {
        PlaceType pt = spotType.streetType.placeType;
        SpotImage image = ImageFactory.getImage(coords, spotType);
        if (PROPERTY_PLACE_TYPES.contains(pt)) {
            return new PropertySpot(image);
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
