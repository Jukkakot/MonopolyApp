package fi.monopoly.presentation.legacy.session.purchase;

import fi.monopoly.application.session.purchase.PropertyPurchaseGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.types.SpotType;

public final class LegacyPropertyPurchaseGateway implements PropertyPurchaseGateway {
    private final Players players;

    public LegacyPropertyPurchaseGateway(fi.monopoly.components.popup.PopupService popupService, Players players) {
        this.players = players;
    }

    @Override
    public boolean buyProperty(Player player, Property property) {
        return player != null && property != null && player.buyProperty(property);
    }

    @Override
    public Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        return players.getPlayers().stream()
                .filter(player -> playerId.equals("player-" + player.getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Property propertyById(String propertyId) {
        return propertyId == null ? null : PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }
}
