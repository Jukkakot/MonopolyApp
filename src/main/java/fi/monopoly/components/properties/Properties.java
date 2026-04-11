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
        return List.copyOf(propertySet);
    }

    public int getTotalHouseCount() {
        int totalHouseCount = 0;
        for (Property property : propertySet) {
            if (property.getSpotType().streetType.placeType == PlaceType.STREET) {
                totalHouseCount += ((StreetProperty) property).getHouseCount();
            }
        }
        return totalHouseCount;
    }

    public int getTotalHotelCount() {
        int totalHotelCount = 0;
        for (Property property : propertySet) {
            if (property.getSpotType().streetType.placeType == PlaceType.STREET) {
                totalHotelCount += ((StreetProperty) property).getHotelCount();
            }
        }
        return totalHotelCount;
    }
}
