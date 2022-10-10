package org.example.spots.Properties;


import org.example.images.PropertyImage;
import org.example.spots.Drawable;
import org.example.types.BuildingType;

import java.util.ArrayList;
import java.util.List;

public class StreetProperty extends Property implements Drawable {
    List<BuildingType> buildingList = new ArrayList<>();
    public StreetProperty(PropertyImage propertyCard) {
        super(propertyCard);
    }
}
