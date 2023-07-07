package org.example.components.properties;


import org.example.types.PlaceType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Properties {
    private final Set<Property> propertySet = new HashSet<>();

    public boolean addProperty(Property property) {
        return propertySet.add(property);
    }

    public List<Property> getProperties() {
        return propertySet.stream().toList();
    }

    private List<StreetProperty> getPropertyTypes(PlaceType placeType) {
        return propertySet.stream().filter(ps -> ps.getSpotType().streetType.placeType.equals(placeType))
                .map(property -> (StreetProperty) property)
                .toList();
    }

    public int getHouseCount() {
        return getPropertyTypes(PlaceType.STREET).stream().mapToInt(StreetProperty::getHouseCount).sum();
    }

    public int getHotelCount() {
        return getPropertyTypes(PlaceType.STREET).stream().mapToInt(StreetProperty::getHotelCount).sum();
    }
}
