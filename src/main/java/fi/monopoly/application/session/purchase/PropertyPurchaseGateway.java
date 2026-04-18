package fi.monopoly.application.session.purchase;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

public interface PropertyPurchaseGateway {
    boolean buyProperty(Player player, Property property);

    default Player playerById(String playerId) {
        return null;
    }

    default Property propertyById(String propertyId) {
        return null;
    }
}
