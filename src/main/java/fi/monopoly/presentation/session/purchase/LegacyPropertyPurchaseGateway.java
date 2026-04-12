package fi.monopoly.presentation.session.purchase;

import fi.monopoly.application.session.purchase.PropertyPurchaseGateway;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.turn.PropertyAuctionResolver;

import java.util.Comparator;
import java.util.List;

public final class LegacyPropertyPurchaseGateway implements PropertyPurchaseGateway {
    private final PopupService popupService;
    private final Players players;

    public LegacyPropertyPurchaseGateway(PopupService popupService, Players players) {
        this.popupService = popupService;
        this.players = players;
    }

    @Override
    public boolean buyProperty(Player player, Property property) {
        return player != null && property != null && player.buyProperty(property);
    }

    @Override
    public void startAuction(Player triggeringPlayer, Property property, CallbackAction onComplete) {
        new PropertyAuctionResolver(popupService, players).resolve(
                triggeringPlayer,
                property,
                PropertyAuctionResolver.AuctionReason.PLAYER_DECLINED,
                onComplete
        );
    }

    @Override
    public List<String> eligibleBidderIds(Player triggeringPlayer) {
        if (players == null) {
            return List.of();
        }
        return players.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .map(player -> "player-" + player.getId())
                .toList();
    }
}
