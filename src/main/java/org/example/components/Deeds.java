package org.example.components;

import lombok.experimental.UtilityClass;
import org.example.components.properties.Property;
import org.example.images.Deed;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Deeds {
    Map<Property, Deed> deedMap = new HashMap<>();

    public Deed getDeed(Property property) {
        if (!deedMap.containsKey(property)) {
            deedMap.put(property, new Deed(property));
        }
        return deedMap.get(property);
    }
}
