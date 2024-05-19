package fi.monopoly.components.spots;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import lombok.experimental.UtilityClass;
import fi.monopoly.images.ImageFactory;
import fi.monopoly.images.SpotImage;

@UtilityClass
public class SpotFactory {

    public Spot getSpot(Coordinates coords, SpotType spotType) {
        PlaceType pt = spotType.streetType.placeType;
        SpotImage image = ImageFactory.getImage(coords, spotType);
        if (spotType.isProperty) {
            if (PlaceType.STREET.equals(pt)) {
                return new StreetPropertySpot(image, spotType);
            } else {
                return new PropertySpot(image, spotType);
            }
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
