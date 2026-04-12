package fi.monopoly.application.session;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;

import java.util.List;

public interface AuctionGateway {
    List<String> eligibleBidderIds(Player triggeringPlayer, Property property);

    Player playerById(String playerId);

    Property propertyById(String propertyId);

    int maxBidFor(Player bidder, Property property);

    int nextBidAmount(Player bidder, Property property, int currentBid);

    boolean transferWinningProperty(Player winner, Property property, int amount);
}
