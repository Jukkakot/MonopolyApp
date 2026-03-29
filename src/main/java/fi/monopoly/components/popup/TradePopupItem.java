package fi.monopoly.components.popup;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

public record TradePopupItem(
        String label,
        TradePopupItemType type,
        Property property,
        Player player,
        boolean selected,
        ButtonAction action
) {
    public static TradePopupItem property(Property property, boolean selected, ButtonAction action) {
        return new TradePopupItem(property.getDisplayName(), TradePopupItemType.PROPERTY, property, null, selected, action);
    }

    public static TradePopupItem money(String label) {
        return new TradePopupItem(label, TradePopupItemType.MONEY, null, null, false, null);
    }

    public static TradePopupItem jailCard(String label) {
        return new TradePopupItem(label, TradePopupItemType.JAIL_CARD, null, null, false, null);
    }

    public static TradePopupItem jailCard(String label, boolean selected, ButtonAction action) {
        return new TradePopupItem(label, TradePopupItemType.JAIL_CARD, null, null, selected, action);
    }

    public static TradePopupItem empty(String label) {
        return new TradePopupItem(label, TradePopupItemType.EMPTY, null, null, false, null);
    }

    public static TradePopupItem player(Player player, ButtonAction action) {
        return new TradePopupItem(player.getName(), TradePopupItemType.PLAYER, null, player, false, action);
    }
}
