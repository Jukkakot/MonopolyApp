package fi.monopoly.components.properties;


import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class Properties {
    private final Set<Property> propertySet = new HashSet<>();
    private List<Property> cachedProperties;

    public boolean addProperty(Property property) {
        boolean added = propertySet.add(property);
        if (added) {
            cachedProperties = null;
        }
        return added;
    }

    public boolean removeProperty(Property property) {
        boolean removed = propertySet.remove(property);
        if (removed) {
            cachedProperties = null;
        }
        return removed;
    }

    public void clear() {
        propertySet.clear();
        cachedProperties = null;
    }

    public List<Property> getProperties() {
        if (cachedProperties == null) {
            cachedProperties = List.copyOf(propertySet);
        }
        return cachedProperties;
    }

    public int countByStreetType(StreetType streetType) {
        int count = 0;
        for (Property property : propertySet) {
            if (property.isSameStreetType(streetType)) {
                count++;
            }
        }
        return count;
    }

    public boolean hasMortgagedPropertyInStreetType(StreetType streetType) {
        for (Property property : propertySet) {
            if (property.isSameStreetType(streetType) && property.isMortgaged()) {
                return true;
            }
        }
        return false;
    }

    public List<StreetProperty> getStreetProperties(StreetType streetType) {
        List<StreetProperty> streetProperties = new ArrayList<>();
        for (Property property : propertySet) {
            if (property.isSameStreetType(streetType)) {
                streetProperties.add((StreetProperty) property);
            }
        }
        streetProperties.sort(java.util.Comparator.comparingInt(property -> property.getSpotType().ordinal()));
        return streetProperties;
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
