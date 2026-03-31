package fi.monopoly.components.trade;

import fi.monopoly.components.properties.Property;

import java.util.ArrayList;
import java.util.List;

public record TradeSelection(
        int moneyAmount,
        List<Property> properties,
        boolean jailCard
) {
    public static final TradeSelection NONE = new TradeSelection(0, List.of(), false);

    public TradeSelection {
        properties = List.copyOf(properties == null ? List.of() : properties);
    }

    public TradeSelection withMoneyAmount(int amount) {
        return new TradeSelection(Math.max(0, amount), properties, jailCard);
    }

    public TradeSelection withAddedProperty(Property nextProperty) {
        if (nextProperty == null || properties.contains(nextProperty)) {
            return this;
        }
        List<Property> nextProperties = new ArrayList<>(properties);
        nextProperties.add(nextProperty);
        return new TradeSelection(moneyAmount, nextProperties, jailCard);
    }

    public TradeSelection withRemovedProperty(Property propertyToRemove) {
        if (propertyToRemove == null || !properties.contains(propertyToRemove)) {
            return this;
        }
        List<Property> nextProperties = new ArrayList<>(properties);
        nextProperties.remove(propertyToRemove);
        return new TradeSelection(moneyAmount, nextProperties, jailCard);
    }

    public boolean containsProperty(Property property) {
        return properties.contains(property);
    }

    public TradeSelection toggleJailCard() {
        return new TradeSelection(moneyAmount, properties, !jailCard);
    }

    public boolean isEmpty() {
        return moneyAmount <= 0 && properties.isEmpty() && !jailCard;
    }
}
