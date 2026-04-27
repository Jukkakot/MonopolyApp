package fi.monopoly.presentation.legacy.session.auction;

import fi.monopoly.application.session.auction.AuctionGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.turn.PropertyAuctionResolver;
import fi.monopoly.types.SpotType;

import java.util.List;


public final class LegacyAuctionGateway implements AuctionGateway {
    private final Players players;
    private final PropertyAuctionResolver auctionResolver;

    public LegacyAuctionGateway(PopupService popupService, Players players) {
        this.players = players;
        this.auctionResolver = new PropertyAuctionResolver(popupService, players);
    }

    @Override
    public List<String> eligibleBidderIds(String triggeringPlayerId, String propertyId) {
        Player triggeringPlayer = playerById(triggeringPlayerId);
        Property property = propertyById(propertyId);
        if (property == null) {
            return List.of();
        }
        return auctionResolver.orderedBidders(triggeringPlayer).stream()
                .filter(player -> auctionResolver.maxBidFor(player, property) >= PropertyAuctionResolver.AUCTION_OPENING_BID)
                .map(player -> "player-" + player.getId())
                .toList();
    }

    @Override
    public int maxBidFor(String bidderId, String propertyId) {
        Player bidder = playerById(bidderId);
        Property property = propertyById(propertyId);
        if (bidder == null || property == null) {
            return 0;
        }
        return auctionResolver.maxBidFor(bidder, property);
    }

    @Override
    public int nextBidAmount(String bidderId, String propertyId, int currentBid) {
        int max = maxBidFor(bidderId, propertyId);
        return auctionResolver.nextBidAmount(max, currentBid);
    }

    @Override
    public boolean transferWinningProperty(String winnerId, String propertyId, int amount) {
        Player winner = playerById(winnerId);
        Property property = propertyById(propertyId);
        return winner != null && property != null && winner.buyProperty(property, amount);
    }

    private Player playerById(String playerId) {
        if (playerId == null || players == null) {
            return null;
        }
        for (Player player : players.getPlayers()) {
            if (playerId.equals("player-" + player.getId())) {
                return player;
            }
        }
        return null;
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
