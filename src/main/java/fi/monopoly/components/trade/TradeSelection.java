package fi.monopoly.components.trade;

import fi.monopoly.components.properties.Property;

public record TradeSelection(
        int moneyAmount,
        Property property,
        boolean jailCard
) {
    public static final TradeSelection NONE = new TradeSelection(0, null, false);

    public boolean isEmpty() {
        return moneyAmount <= 0 && property == null && !jailCard;
    }
}
