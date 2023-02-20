package org.example.components;


import org.example.components.spots.PropertySpot;
import org.example.images.ImageFactory;
import org.example.images.SpotImage;
import org.example.types.StreetType;

import java.util.*;

public class Deeds {
    private final Map<StreetType, List<SpotImage>> deedList = new HashMap<>();

    public void addDeed(PropertySpot propertySpot) {
        StreetType st = propertySpot.getSpotType().streetType;
        if (!deedList.containsKey(st)) {
            deedList.put(st, new ArrayList<>());
        }

        deedList.get(st).add(ImageFactory.getImage(propertySpot));
    }

    public List<SpotImage> getDeeds(StreetType pt) {
        return deedList.get(pt);
    }

    public List<SpotImage> getAllDeeds() {
        return deedList.values().stream().flatMap(Collection::stream).toList();
    }

    public Map<StreetType, List<SpotImage>> getDeeds() {
        return deedList;
    }
}
