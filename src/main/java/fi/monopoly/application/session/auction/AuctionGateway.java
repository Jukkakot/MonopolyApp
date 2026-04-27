package fi.monopoly.application.session.auction;

import java.util.List;

public interface AuctionGateway {
    List<String> eligibleBidderIds(String triggeringPlayerId, String propertyId);

    int maxBidFor(String bidderId, String propertyId);

    int nextBidAmount(String bidderId, String propertyId, int currentBid);

    boolean transferWinningProperty(String winnerId, String propertyId, int amount);
}
