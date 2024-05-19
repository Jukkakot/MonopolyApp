package fi.monopoly.components;

import lombok.experimental.UtilityClass;
import fi.monopoly.components.properties.Property;
import fi.monopoly.images.Deed;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class DeedFactor {
    Map<Property, Deed> deedMap = new HashMap<>();

    public Deed getDeed(Property property) {
        if (!deedMap.containsKey(property)) {
            deedMap.put(property, new Deed(property));
        }
        return deedMap.get(property);
    }
}
