package fi.monopoly.components.turn;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

public record OfferToBuyPropertyEffect(Player player, Property property, String message) implements TurnEffect {
}
