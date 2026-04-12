package fi.monopoly.application.session.purchase;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

import java.util.List;

public interface PropertyPurchaseGateway {
    boolean buyProperty(Player player, Property property);

    void startAuction(Player triggeringPlayer, Property property, CallbackAction onComplete);

    List<String> eligibleBidderIds(Player triggeringPlayer);
}
