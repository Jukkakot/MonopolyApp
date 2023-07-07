package org.example.components.properties;

import lombok.experimental.UtilityClass;
import org.example.types.PlaceType;
import org.example.types.SpotType;

@UtilityClass
public class PropertyFactory {
    public Property getProperty(SpotType spotType) {
        PlaceType placeType = spotType.streetType.placeType;
        if (placeType.equals(PlaceType.STREET)) {
            return new StreetProperty(spotType);
        } else if (placeType.equals(PlaceType.RAILROAD)) {
            return new RailRoadProperty(spotType);
        } else if (placeType.equals(PlaceType.UTILITY)) {
            return new UtilityProperty(spotType);
        }
        throw new IllegalArgumentException("Can't make property out of SpotType" + spotType);
    }
}
