package fi.monopoly.components.trade;

import fi.monopoly.components.properties.Property;

public record TradeSelection(
        int moneyAmount,
        Property property,
        boolean jailCard
) {
    public static final TradeSelection NONE = new TradeSelection(0, null, false);

    public TradeSelection withMoneyAmount(int amount) {
        return new TradeSelection(Math.max(0, amount), property, jailCard);
    }

    public TradeSelection withProperty(Property nextProperty) {
        return new TradeSelection(moneyAmount, nextProperty, jailCard);
    }

    public TradeSelection toggleJailCard() {
        return new TradeSelection(moneyAmount, property, !jailCard);
    }

    public boolean isEmpty() {
        return moneyAmount <= 0 && property == null && !jailCard;
    }
}
