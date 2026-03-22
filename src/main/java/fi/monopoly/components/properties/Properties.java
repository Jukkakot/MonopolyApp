package fi.monopoly.components.properties;


import fi.monopoly.types.PlaceType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Properties {
    private final Set<Property> propertySet = new HashSet<>();

    public boolean addProperty(Property property) {
        return propertySet.add(property);
    }

    public boolean removeProperty(Property property) {
        return propertySet.remove(property);
    }

    public void clear() {
        propertySet.clear();
    }

    public List<Property> getProperties() {
        return propertySet.stream().toList();
    }

    private List<StreetProperty> getPropertyTypes(PlaceType placeType) {
        return propertySet.stream().filter(ps -> ps.getSpotType().streetType.placeType.equals(placeType))
                .map(property -> (StreetProperty) property)
                .toList();
    }

    public int getTotalHouseCount() {
        return getPropertyTypes(PlaceType.STREET).stream().mapToInt(StreetProperty::getHouseCount).sum();
    }

    public int getTotalHotelCount() {
        return getPropertyTypes(PlaceType.STREET).stream().mapToInt(StreetProperty::getHotelCount).sum();
    }
}
