package org.example.components;


import org.example.components.spots.PropertySpot;
import org.example.types.StreetType;

import java.util.*;

public class Deeds {
    private final Map<StreetType, List<PropertySpot>> deedList = new HashMap<>();

    public void addDeed(PropertySpot spot) {
        StreetType st = spot.getSpotType().streetType;
        if (!deedList.containsKey(st)) {
            deedList.put(st, new ArrayList<>());
        }
        deedList.get(st).add(spot);
    }

    public List<PropertySpot> getDeeds(StreetType pt) {
        return deedList.get(pt);
    }

    public List<PropertySpot> getAllDeeds() {
        return deedList.values().stream().flatMap(Collection::stream).toList();
    }

    public Map<StreetType, List<PropertySpot>> getDeeds() {
        return deedList;
    }
}
