package fi.monopoly.presentation.session.purchase;

import fi.monopoly.application.session.purchase.PropertyPurchaseGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

public final class LegacyPropertyPurchaseGateway implements PropertyPurchaseGateway {
    public LegacyPropertyPurchaseGateway(fi.monopoly.components.popup.PopupService popupService, fi.monopoly.components.Players players) {
    }

    @Override
    public boolean buyProperty(Player player, Property property) {
        return player != null && property != null && player.buyProperty(property);
    }
}
