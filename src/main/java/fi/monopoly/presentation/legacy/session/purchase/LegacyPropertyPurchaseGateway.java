package fi.monopoly.presentation.legacy.session.purchase;

import fi.monopoly.application.session.purchase.PropertyPurchaseGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LegacyPropertyPurchaseGateway implements PropertyPurchaseGateway {
    private final Players players;

    @Override
    public boolean buyProperty(String playerId, String propertyId) {
        Player player = playerById(playerId);
        Property property = propertyById(propertyId);
        return player != null && property != null && player.buyProperty(property);
    }

    private Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        return players.getPlayers().stream()
                .filter(p -> playerId.equals("player-" + p.getId()))
                .findFirst()
                .orElse(null);
    }

    private Property propertyById(String propertyId) {
        if (propertyId == null) {
            return null;
        }
        try {
            return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
