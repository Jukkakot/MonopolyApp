package fi.monopoly.application.session.purchase;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

public interface PropertyPurchaseGateway {
    boolean buyProperty(Player player, Property property);
}
