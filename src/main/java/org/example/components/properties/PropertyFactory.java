package org.example.components.properties;

import lombok.experimental.UtilityClass;
import org.example.types.PlaceType;
import org.example.types.SpotType;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class PropertyFactory {
    private static Map<SpotType, Property> propertyMap = new HashMap<>();

    public Property getProperty(SpotType spotType) {
        if (!propertyMap.containsKey(spotType)) {
            propertyMap.put(spotType, addProperty(spotType));
        }
        return propertyMap.get(spotType);
    }

    private Property addProperty(SpotType spotType) {
        PlaceType placeType = spotType.streetType.placeType;
        if (placeType.equals(PlaceType.STREET)) {
            return new StreetProperty(spotType);
        } else if (placeType.equals(PlaceType.RAILROAD)) {
            return new RailRoadProperty(spotType);
        } else if (placeType.equals(PlaceType.UTILITY)) {
            return new UtilityProperty(spotType);
        }
        throw new IllegalArgumentException("Can't make property out of SpotType " + spotType);
    }
}
