package org.example.components.deeds;


import org.example.components.spots.propertySpots.PropertySpot;
import org.example.components.spots.propertySpots.StreetPropertySpot;
import org.example.images.DeedImage;
import org.example.types.PlaceType;
import org.example.types.StreetType;

import java.util.*;

public class Deeds {
    private final Map<StreetType, List<DeedImage>> deedList = new HashMap<>();
    private final Set<PropertySpot> spotList = new HashSet<>();

    public void addDeed(PropertySpot propertySpot) {
        StreetType st = propertySpot.getSpotType().streetType;
        if (!deedList.containsKey(st)) {
            deedList.put(st, new ArrayList<>());
        }
        spotList.add(propertySpot);
        deedList.get(st).add(new DeedImage(propertySpot.getImage()));
    }

    public List<DeedImage> getDeeds(StreetType pt) {
        return deedList.get(pt);
    }

    public List<DeedImage> getAllDeeds() {
        return deedList.values().stream().flatMap(Collection::stream).toList();
    }

    public Map<StreetType, List<DeedImage>> getDeeds() {
        return deedList;
    }
    private List<StreetPropertySpot> getPlaceTypes(PlaceType placeType) {
        return spotList.stream().filter(ps -> ps.getSpotType().streetType.placeType.equals(placeType))
                .map(propertySpot -> (StreetPropertySpot) propertySpot)
                .toList();
    }
    public int getHouseCount() {
        return getPlaceTypes(PlaceType.STREET).stream().mapToInt(StreetPropertySpot::getHouseCount).sum();
    }
    public int getHotelCount() {
        return getPlaceTypes(PlaceType.STREET).stream().mapToInt(StreetPropertySpot::getHotelCount).sum();
    }
}
