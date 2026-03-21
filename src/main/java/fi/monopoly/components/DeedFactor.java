package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import lombok.experimental.UtilityClass;
import fi.monopoly.components.properties.Property;
import fi.monopoly.images.Deed;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class DeedFactor {
    Map<Property, Deed> deedMap = new HashMap<>();

    public Deed getDeed(MonopolyRuntime runtime, Property property) {
        if (!deedMap.containsKey(property)) {
            deedMap.put(property, new Deed(runtime, property));
        }
        return deedMap.get(property);
    }
}
