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
    public List<String> eligibleBidderIds(Player triggeringPlayer, Property property) {
        return auctionResolver.orderedBidders(triggeringPlayer).stream()
                .filter(player -> auctionResolver.maxBidFor(player, property) >= PropertyAuctionResolver.AUCTION_OPENING_BID)
                .map(player -> "player-" + player.getId())
                .toList();
    }

    @Override
    public Player playerById(String playerId) {
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

    @Override
    public Property propertyById(String propertyId) {
        if (propertyId == null) {
            return null;
        }
        return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }

    @Override
    public int maxBidFor(Player bidder, Property property) {
        return auctionResolver.maxBidFor(bidder, property);
    }

    @Override
    public int nextBidAmount(Player bidder, Property property, int currentBid) {
        return auctionResolver.nextBidAmount(maxBidFor(bidder, property), currentBid);
    }

    @Override
    public boolean transferWinningProperty(Player winner, Property property, int amount) {
        return winner != null && property != null && winner.buyProperty(property, amount);
    }
}
