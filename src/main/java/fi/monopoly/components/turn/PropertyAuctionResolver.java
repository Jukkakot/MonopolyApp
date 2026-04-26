package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.StreetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
@RequiredArgsConstructor
public class PropertyAuctionResolver {
    public static final int AUCTION_BID_INCREMENT = 10;
    public static final int AUCTION_OPENING_BID = 10;
    private static final int DEFAULT_RESERVE = 100;
    private static AuctionMetrics metrics = new AuctionMetrics(0, 0, 0);
    private static final StrongBotConfig STRONG_CONFIG = StrongBotConfig.defaults();

    private final PopupService popupService;
    private final Players players;

    public void resolve(Player triggeringPlayer, Property property, CallbackAction onComplete) {
        resolve(triggeringPlayer, property, AuctionContext.bankAuction(), onComplete);
    }

    public void resolve(Player triggeringPlayer, Property property, AuctionReason reason, CallbackAction onComplete) {
        resolve(triggeringPlayer, property, AuctionContext.of(reason, triggeringPlayer), onComplete);
    }

    private void resolve(Player triggeringPlayer, Property property, AuctionContext auctionContext, CallbackAction onComplete) {
        List<AuctionParticipant> orderedPlayers = orderedBidders(triggeringPlayer).stream()
                .map(bidder -> new AuctionParticipant(bidder, maxBidFor(bidder, property)))
                .filter(participant -> participant.maxBid() >= AUCTION_OPENING_BID)
                .toList();
        if (orderedPlayers.isEmpty()) {
            popupService.show(text("property.auction.noBids", property.getDisplayName()), onComplete::doAction);
            return;
        }

        if (orderedPlayers.stream().noneMatch(participant -> participant.player().getComputerProfile() == ComputerPlayerProfile.HUMAN)) {
            finalizeWinningBid(determineWinningBid(orderedPlayers), property, onComplete);
            return;
        }

        runInteractiveAuction(property, orderedPlayers, new InteractiveAuctionState(0, null, 0, new HashSet<>()), auctionContext, onComplete);
    }

    public static synchronized void resetMetrics() {
        metrics = new AuctionMetrics(0, 0, 0);
    }

    public static synchronized AuctionMetrics metrics() {
        return metrics;
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
        resolve(null, property, AuctionContext.bankAuction(), () -> resolveNext(properties, index + 1, onComplete));
    }

    private AuctionBid determineWinningBid(List<AuctionParticipant> orderedPlayers) {
        List<AuctionBid> bids = new ArrayList<>();
        for (AuctionParticipant participant : orderedPlayers) {
            Player bidder = participant.player();
            int maxBid = participant.maxBid();
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

    private void runInteractiveAuction(
            Property property,
            List<AuctionParticipant> bidders,
            InteractiveAuctionState state,
            AuctionContext auctionContext,
            CallbackAction onComplete
    ) {
        if (bidders.isEmpty() || state.passedPlayers().size() >= bidders.size()) {
            popupService.show(text("property.auction.noBids", property.getDisplayName()), onComplete::doAction);
            return;
        }
        long activeCount = bidders.stream()
                .filter(participant -> !state.passedPlayers().contains(participant.player()))
                .count();
        if (state.currentWinner() != null && activeCount <= 1) {
            finalizeWinningBid(new AuctionBid(state.currentWinner(), state.currentBid()), property, onComplete);
            return;
        }

        AuctionParticipant nextParticipant = nextActiveBidder(bidders, state);
        if (nextParticipant == null) {
            finalizeWinningBid(state.currentWinner() == null ? null : new AuctionBid(state.currentWinner(), state.currentBid()), property, onComplete);
            return;
        }

        int minBid = state.currentBid() == 0 ? AUCTION_OPENING_BID : state.currentBid() + AUCTION_BID_INCREMENT;
        if (nextParticipant.maxBid() < minBid) {
            Set<Player> passedPlayers = new HashSet<>(state.passedPlayers());
            passedPlayers.add(nextParticipant.player());
            runInteractiveAuction(property, bidders, state.withPass(nextParticipant.player(), nextParticipant.index() + 1, passedPlayers), auctionContext, onComplete);
            return;
        }

        if (nextParticipant.player().getComputerProfile() != ComputerPlayerProfile.HUMAN) {
            int botBid = nextBidAmount(nextParticipant.maxBid(), state.currentBid());
            runInteractiveAuction(
                    property,
                    bidders,
                    state.withBid(nextParticipant.player(), botBid, nextParticipant.index() + 1),
                    auctionContext,
                    onComplete
            );
            return;
        }

        int maxBid = nextParticipant.maxBid();
        popupService.showPropertyAuction(
                property,
                text("property.auction.prompt", nextParticipant.player().getName(), property.getDisplayName()),
                state.currentWinner(),
                state.currentBid(),
                text("property.auction.bid", minBid),
                text("property.auction.pass"),
                () -> runInteractiveAuction(
                        property,
                        bidders,
                        state.withBid(nextParticipant.player(), minBid, nextParticipant.index() + 1),
                        auctionContext,
                        onComplete
                ),
                () -> {
                    Set<Player> passedPlayers = new HashSet<>(state.passedPlayers());
                    passedPlayers.add(nextParticipant.player());
                    runInteractiveAuction(
                            property,
                            bidders,
                            state.withPass(nextParticipant.player(), nextParticipant.index() + 1, passedPlayers),
                            auctionContext,
                            onComplete
                    );
                }
        );
    }

    private AuctionParticipant nextActiveBidder(List<AuctionParticipant> bidders, InteractiveAuctionState state) {
        for (int offset = 0; offset < bidders.size(); offset++) {
            int index = (state.nextIndex() + offset) % bidders.size();
            AuctionParticipant participant = bidders.get(index);
            if (!state.passedPlayers().contains(participant.player())) {
                return participant.withIndex(index);
            }
        }
        return null;
    }

    public int nextBidAmount(int maxBid, int currentBid) {
        int minBid = currentBid == 0 ? AUCTION_OPENING_BID : currentBid + AUCTION_BID_INCREMENT;
        if (maxBid <= minBid) {
            return minBid;
        }
        int headroom = maxBid - minBid;
        int extraStep = Math.max(
                AUCTION_BID_INCREMENT,
                roundDownToIncrement(Math.max(AUCTION_BID_INCREMENT, headroom / 3))
        );
        return Math.min(maxBid, minBid + extraStep);
    }

    public List<Player> orderedBidders(Player triggeringPlayer) {
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

    public int maxBidFor(Player bidder, Property property) {
        int cashLimit = bidder.getComputerProfile() == ComputerPlayerProfile.HUMAN
                ? bidder.getMoneyAmount()
                : bidder.getMoneyAmount() - reserveFor(bidder);
        if (cashLimit < AUCTION_OPENING_BID) {
            return 0;
        }
        int valuation = switch (bidder.getComputerProfile()) {
            case STRONG -> strongBidLimit(bidder, property, cashLimit);
            case SMOKE_TEST -> Math.min(cashLimit, property.getPrice());
            case HUMAN -> cashLimit;
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
            multiplier += STRONG_CONFIG.auctionSetCompletionBonus() / (double) Math.max(1, property.getPrice());
        } else if (ownedInSet > 0) {
            multiplier += 0.2;
        }
        if (bestOpponentCount(bidder, property.getSpotType().streetType) >= setSize - 1 && setSize > 1) {
            multiplier += 0.25 * STRONG_CONFIG.opponentLeaderPressure();
        }
        if (property.getSpotType().streetType.placeType == fi.monopoly.types.PlaceType.RAILROAD) {
            multiplier += ownedInSet * STRONG_CONFIG.railroadCompletionWeight() / 100.0;
        }
        if (property.getSpotType().streetType.placeType == fi.monopoly.types.PlaceType.UTILITY) {
            multiplier += ownedInSet * STRONG_CONFIG.utilityCompletionWeight() / 100.0;
        }
        multiplier += (STRONG_CONFIG.auctionAggression() - 1.0);
        multiplier *= STRONG_CONFIG.colorGroupWeight(property.getSpotType().streetType);
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
        return bidder.getComputerProfile() == ComputerPlayerProfile.STRONG
                ? STRONG_CONFIG.dangerCashReserve() + 50
                : DEFAULT_RESERVE;
    }

    private int roundDownToIncrement(int amount) {
        if (amount < AUCTION_OPENING_BID) {
            return 0;
        }
        return Math.max(AUCTION_OPENING_BID, (amount / AUCTION_BID_INCREMENT) * AUCTION_BID_INCREMENT);
    }

    public static synchronized void recordAuctionOutcome(Property property, int winningBid) {
        metrics = new AuctionMetrics(
                metrics.completedAuctions() + 1,
                metrics.totalMarketPrice() + property.getPrice(),
                metrics.totalWinningBid() + winningBid
        );
    }

    private void finalizeWinningBid(AuctionBid winningBid, Property property, CallbackAction onComplete) {
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
        recordAuctionOutcome(property, winningBid.amount());
    }

    private record AuctionBid(Player player, int amount) {
    }

    private record AuctionParticipant(Player player, int maxBid, int index) {
        private AuctionParticipant(Player player, int maxBid) {
            this(player, maxBid, -1);
        }

        private AuctionParticipant withIndex(int index) {
            return new AuctionParticipant(player, maxBid, index);
        }
    }

    public enum AuctionReason {
        PLAYER_DECLINED("property.auction.reason.declined"),
        PLAYER_COULD_NOT_PAY("property.auction.reason.couldNotPay"),
        BANK_AUCTION("property.auction.reason.bank");

        private final String textKey;

        AuctionReason(String textKey) {
            this.textKey = textKey;
        }

        private String message(String playerName) {
            return switch (this) {
                case PLAYER_DECLINED, PLAYER_COULD_NOT_PAY -> text(textKey, playerName);
                case BANK_AUCTION -> text(textKey);
            };
        }
    }

    private record AuctionContext(AuctionReason reason, String triggeringPlayerName) {
        private static AuctionContext of(AuctionReason reason, Player triggeringPlayer) {
            return new AuctionContext(reason, triggeringPlayer == null ? "" : triggeringPlayer.getName());
        }

        private static AuctionContext bankAuction() {
            return new AuctionContext(AuctionReason.BANK_AUCTION, "");
        }

        private String reasonMessage() {
            return reason.message(triggeringPlayerName);
        }
    }

    private record InteractiveAuctionState(
            int currentBid,
            Player currentWinner,
            int nextIndex,
            Set<Player> passedPlayers
    ) {
        private InteractiveAuctionState withBid(Player bidder, int bid, int nextIndex) {
            return new InteractiveAuctionState(bid, bidder, nextIndex, new HashSet<>(passedPlayers));
        }

        private InteractiveAuctionState withPass(Player bidder, int nextIndex, Set<Player> passedPlayers) {
            return new InteractiveAuctionState(currentBid, currentWinner, nextIndex, passedPlayers);
        }
    }

    public record AuctionMetrics(
            int completedAuctions,
            int totalMarketPrice,
            int totalWinningBid
    ) {
        public double discountRate() {
            if (completedAuctions <= 0 || totalMarketPrice <= 0) {
                return 0.0;
            }
            return 1.0 - (totalWinningBid / (double) totalMarketPrice);
        }
    }
}
