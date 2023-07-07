package org.example.components.deeds;


import org.example.components.properties.Property;
import org.example.components.properties.StreetProperty;
import org.example.images.Deed;
import org.example.types.PlaceType;
import org.example.types.StreetType;

import java.util.*;


public class Deeds {
    private final Map<StreetType, List<Deed>> deedList = new HashMap<>();
    private final Set<Property> propertySet = new HashSet<>();

    public void addDeed(Property property) {
        StreetType st = property.getSpotType().streetType;
        if (!deedList.containsKey(st)) {
            deedList.put(st, new ArrayList<>());
        }
        propertySet.add(property);
        deedList.get(st).add(new Deed(property));
    }

    public List<Deed> getDeeds(StreetType pt) {
        return deedList.get(pt);
    }

    public List<Deed> getAllDeeds() {
        return deedList.values().stream().flatMap(Collection::stream).toList();
    }

    public Map<StreetType, List<Deed>> getDeeds() {
        return deedList;
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
