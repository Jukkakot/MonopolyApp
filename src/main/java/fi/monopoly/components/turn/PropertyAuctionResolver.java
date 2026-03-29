package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.StreetType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class PropertyAuctionResolver {
    private static final int AUCTION_BID_INCREMENT = 10;
    private static final int AUCTION_OPENING_BID = 10;
    private static final int DEFAULT_RESERVE = 100;
    private static final int STRONG_RESERVE = 200;

    private final PopupService popupService;
    private final Players players;

    public PropertyAuctionResolver(PopupService popupService, Players players) {
        this.popupService = popupService;
        this.players = players;
    }

    public void resolve(Player triggeringPlayer, Property property, CallbackAction onComplete) {
        AuctionBid winningBid = determineWinningBid(triggeringPlayer, property);
        if (winningBid == null) {
            popupService.show(text("property.auction.noBids", property.getDisplayName()), onComplete::doAction);
            return;
        }
        boolean bought = winningBid.player().buyProperty(property, winningBid.amount());
        if (!bought) {
            log.warn("Auction winner {} could not buy {} for M{} after bidding",
                    winningBid.player().getName(), property.getDisplayName(), winningBid.amount());
            popupService.show(text("property.auction.noBids", property.getDisplayName()), onComplete::doAction);
            return;
        }
        popupService.show(
                text("property.auction.won", winningBid.player().getName(), property.getDisplayName(), winningBid.amount()),
                onComplete::doAction
        );
    }

    public void resolveAll(List<Property> properties, CallbackAction onComplete) {
        resolveNext(properties, 0, onComplete);
    }

    private void resolveNext(List<Property> properties, int index, CallbackAction onComplete) {
        if (properties == null || index >= properties.size()) {
            onComplete.doAction();
            return;
        }
        Property property = properties.get(index);
        resolve(null, property, () -> resolveNext(properties, index + 1, onComplete));
    }

    private AuctionBid determineWinningBid(Player triggeringPlayer, Property property) {
        if (players == null) {
            return null;
        }
        List<Player> orderedPlayers = orderedBidders(triggeringPlayer);
        if (orderedPlayers.isEmpty()) {
            return null;
        }

        List<AuctionBid> bids = new ArrayList<>();
        for (Player bidder : orderedPlayers) {
            int maxBid = maxBidFor(bidder, property);
            if (maxBid >= AUCTION_OPENING_BID) {
                bids.add(new AuctionBid(bidder, maxBid));
            }
        }
        if (bids.isEmpty()) {
            return null;
        }

        AuctionBid highest = bids.get(0);
        for (int i = 1; i < bids.size(); i++) {
            AuctionBid candidate = bids.get(i);
            if (candidate.amount() > highest.amount()) {
                highest = candidate;
            }
        }
        if (highest == null) {
            return null;
        }

        AuctionBid winningBid = highest;
        int secondHighest = bids.stream()
                .filter(bid -> bid.player() != winningBid.player())
                .mapToInt(AuctionBid::amount)
                .max()
                .orElse(0);
        int winningBidAmount = Math.min(winningBid.amount(), Math.max(AUCTION_OPENING_BID, secondHighest + AUCTION_BID_INCREMENT));
        return new AuctionBid(winningBid.player(), roundDownToIncrement(winningBidAmount));
    }

    private List<Player> orderedBidders(Player triggeringPlayer) {
        List<Player> sortedPlayers = players.getPlayers().stream()
                .sorted(Comparator.comparingInt(Player::getTurnNumber))
                .toList();
        if (sortedPlayers.isEmpty()) {
            return List.of();
        }
        int triggerIndex = sortedPlayers.indexOf(triggeringPlayer);
        if (triggerIndex < 0) {
            return sortedPlayers;
        }
        List<Player> ordered = new ArrayList<>(sortedPlayers.size());
        for (int offset = 1; offset <= sortedPlayers.size(); offset++) {
            ordered.add(sortedPlayers.get((triggerIndex + offset) % sortedPlayers.size()));
        }
        return ordered;
    }

    private int maxBidFor(Player bidder, Property property) {
        int cashLimit = bidder.getMoneyAmount() - reserveFor(bidder);
        if (cashLimit < AUCTION_OPENING_BID) {
            return 0;
        }
        int valuation = switch (bidder.getComputerProfile()) {
            case STRONG -> strongBidLimit(bidder, property, cashLimit);
            case SMOKE_TEST, HUMAN -> Math.min(cashLimit, property.getPrice());
        };
        return roundDownToIncrement(Math.min(cashLimit, valuation));
    }

    private int strongBidLimit(Player bidder, Property property, int cashLimit) {
        double multiplier = switch (property.getSpotType().streetType.placeType) {
            case STREET -> property.getRent(bidder) >= 20 ? 1.0 : 0.85;
            case RAILROAD -> 1.1;
            case UTILITY -> 0.75;
            default -> 0.8;
        };
        int ownedInSet = bidder.getOwnedProperties().stream()
                .filter(ownedProperty -> ownedProperty.getSpotType().streetType == property.getSpotType().streetType)
                .mapToInt(ignored -> 1)
                .sum();
        int setSize = setSize(property.getSpotType().streetType);
        if (ownedInSet + 1 >= setSize && setSize > 0) {
            multiplier += 0.55;
        } else if (ownedInSet > 0) {
            multiplier += 0.2;
        }
        if (bestOpponentCount(bidder, property.getSpotType().streetType) >= setSize - 1 && setSize > 1) {
            multiplier += 0.25;
        }
        return (int) Math.floor(Math.min(cashLimit, property.getPrice() * multiplier));
    }

    private int bestOpponentCount(Player bidder, StreetType streetType) {
        return players.getPlayers().stream()
                .filter(player -> player != bidder)
                .mapToInt(player -> (int) player.getOwnedProperties().stream()
                        .filter(property -> property.getSpotType().streetType == streetType)
                        .count())
                .max()
                .orElse(0);
    }

    private int setSize(StreetType streetType) {
        return switch (streetType.placeType) {
            case STREET -> streetType == StreetType.BROWN || streetType == StreetType.DARK_BLUE ? 2 : 3;
            case RAILROAD -> 4;
            case UTILITY -> 2;
            default -> 0;
        };
    }

    private int reserveFor(Player bidder) {
        return bidder.getComputerProfile() == ComputerPlayerProfile.STRONG ? STRONG_RESERVE : DEFAULT_RESERVE;
    }

    private int roundDownToIncrement(int amount) {
        if (amount < AUCTION_OPENING_BID) {
            return 0;
        }
        return Math.max(AUCTION_OPENING_BID, (amount / AUCTION_BID_INCREMENT) * AUCTION_BID_INCREMENT);
    }

    private record AuctionBid(Player player, int amount) {
    }
}
