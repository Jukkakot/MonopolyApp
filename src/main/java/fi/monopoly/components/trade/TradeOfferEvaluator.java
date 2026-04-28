package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

public final class TradeOfferEvaluator {
    private static final int JAIL_CARD_VALUE = 60;

    public TradeDecision evaluateForRecipient(TradeOffer offer) {
        return evaluateForRecipient(offer, BotTradeProfile.BALANCED);
    }

    public TradeDecision evaluateForRecipient(TradeOffer offer, BotTradeProfile profile) {
        return evaluateForRecipient(offer, profile, null);
    }

    public TradeDecision evaluateForRecipient(TradeOffer offer, BotTradeProfile profile, StrongBotConfig strongConfig) {
        if (!offer.isValid()) {
            return new TradeDecision(false, -10_000, "Reject trade: invalid offer");
        }
        Player recipient = offer.recipient();
        double gainScore = selectionValue(recipient, offer.offeredToRecipient(), true, strongConfig);
        double lossScore = selectionValue(recipient, offer.requestedFromRecipient(), false, strongConfig);
        if (strongConfig != null && isLeadingOpponent(offer.proposer(), recipient)) {
            gainScore *= 1.0 + Math.max(0, strongConfig.opponentLeaderPressure() - 1.0) * 0.5;
            lossScore *= strongConfig.opponentLeaderPressure();
        }
        double score = gainScore - lossScore;
        int acceptThreshold = profile.getAcceptThreshold() - (strongConfig == null ? 0 : strongConfig.tradeFairnessTolerance());
        boolean accept = score >= acceptThreshold;
        boolean tooFarForCounter = score < profile.getCounterOfferFloor();
        return new TradeDecision(
                accept,
                score,
                accept
                        ? "Accept trade (" + profile.name() + "): receive value " + round(gainScore) + " vs give " + round(lossScore)
                        : tooFarForCounter
                        ? "Reject trade (" + profile.name() + "): too one-sided for a counteroffer, receive value "
                        + round(gainScore) + " vs give " + round(lossScore)
                        : "Reject trade (" + profile.name() + "): receive value " + round(gainScore) + " vs give " + round(lossScore)
        );
    }

    public TradeOffer proposeCounterOffer(TradeOffer offer, BotTradeProfile profile) {
        return proposeCounterOffer(offer, profile, null);
    }

    public TradeOffer proposeCounterOffer(TradeOffer offer, BotTradeProfile profile, StrongBotConfig strongConfig) {
        TradeDecision currentDecision = evaluateForRecipient(offer, profile, strongConfig);
        if (currentDecision.accept()) {
            return null;
        }
        if (currentDecision.score() < profile.getCounterOfferFloor()) {
            return null;
        }
        int acceptThreshold = profile.getAcceptThreshold() - (strongConfig == null ? 0 : strongConfig.tradeFairnessTolerance());
        int adjustment = Math.min(profile.getMaxCounterAdjustment(), roundUpToNearestTen(acceptThreshold - currentDecision.score()));
        if (adjustment <= 0) {
            return null;
        }

        TradeSelection offeredToRecipient = offer.offeredToRecipient();
        TradeSelection requestedFromRecipient = offer.requestedFromRecipient();
        int remainingAdjustment = adjustment;

        int reducibleRequestedMoney = Math.min(requestedFromRecipient.moneyAmount(), remainingAdjustment);
        if (reducibleRequestedMoney > 0) {
            requestedFromRecipient = requestedFromRecipient.withMoneyAmount(requestedFromRecipient.moneyAmount() - reducibleRequestedMoney);
            remainingAdjustment -= reducibleRequestedMoney;
        }

        int proposerHeadroom = Math.max(0, offer.proposer().getMoneyAmount() - offeredToRecipient.moneyAmount());
        int additionalOfferedMoney = Math.min(proposerHeadroom, remainingAdjustment);
        if (additionalOfferedMoney > 0) {
            offeredToRecipient = offeredToRecipient.withMoneyAmount(offeredToRecipient.moneyAmount() + additionalOfferedMoney);
            remainingAdjustment -= additionalOfferedMoney;
        }

        if (remainingAdjustment > 0) {
            return null;
        }

        TradeOffer counterOffer = offer
                .withOfferedToRecipient(offeredToRecipient)
                .withRequestedFromRecipient(requestedFromRecipient);
        if (!counterOffer.isValid()) {
            return null;
        }
        return evaluateForRecipient(counterOffer, profile, strongConfig).accept() ? counterOffer : null;
    }

    public int estimateNetDeltaForRecipient(TradeOffer offer) {
        return estimateNetDeltaForRecipient(offer, null);
    }

    public int estimateNetDeltaForRecipient(TradeOffer offer, StrongBotConfig strongConfig) {
        Player recipient = offer.recipient();
        double gainScore = selectionValue(recipient, offer.offeredToRecipient(), true, strongConfig);
        double lossScore = selectionValue(recipient, offer.requestedFromRecipient(), false, strongConfig);
        return (int) Math.round(gainScore - lossScore);
    }

    public int estimateSelectionMarketValue(TradeSelection selection) {
        int value = selection.moneyAmount();
        if (selection.jailCard()) {
            value += JAIL_CARD_VALUE;
        }
        for (Property property : selection.properties()) {
            value += propertyMarketValue(property);
        }
        return value;
    }

    private double selectionValue(Player perspective, TradeSelection selection, boolean receiving, StrongBotConfig strongConfig) {
        double value = selection.moneyAmount() * (strongConfig == null ? 1.0 : strongConfig.tradeLiquidityWeight());
        if (selection.jailCard()) {
            value += JAIL_CARD_VALUE;
        }
        for (Property property : selection.properties()) {
            value += propertyValue(perspective, property, receiving, strongConfig);
        }
        return value;
    }

    private double propertyValue(Player perspective, Property property, boolean receiving, StrongBotConfig strongConfig) {
        double value = property.getLiquidationValue();
        if (property.isMortgaged()) {
            value -= property.getMortgageInterest();
        }
        StreetType streetType = property.getSpotType().streetType;
        int ownedInSet = perspective.countOwnedProperties(streetType);
        if (receiving) {
            ownedInSet += 1;
        }
        int setSize = fi.monopoly.types.SpotType.getNumberOfSpots(streetType);
        if (streetType.placeType == PlaceType.STREET && ownedInSet >= setSize) {
            value += strongConfig == null ? 220 : strongConfig.tradeSetCompletionWeight();
        } else if (ownedInSet > 0) {
            value += 70;
        }
        if (!receiving && perspective.ownsAllStreetProperties(streetType)) {
            value += strongConfig == null ? 220 : strongConfig.tradeSetCompletionWeight();
        }
        if (streetType.placeType == PlaceType.RAILROAD) {
            value += ownedInSet * 35;
            if (strongConfig != null) {
                value += ownedInSet * strongConfig.railroadCompletionWeight();
            }
        }
        if (streetType.placeType == PlaceType.UTILITY && ownedInSet >= 2) {
            value += 50;
        }
        if (streetType.placeType == PlaceType.UTILITY && strongConfig != null) {
            value += ownedInSet * strongConfig.utilityCompletionWeight();
        }
        if (strongConfig != null) {
            value *= strongConfig.colorGroupWeight(streetType);
        }
        return value;
    }

    private int propertyMarketValue(Property property) {
        int value = property.getPrice();
        if (property.isMortgaged()) {
            value -= property.getMortgageInterest();
        }
        return Math.max(0, value);
    }

    private boolean isLeadingOpponent(Player proposer, Player recipient) {
        return proposer.getMoneyAmount() + proposer.getTotalLiquidationValue()
                > recipient.getMoneyAmount() + recipient.getTotalLiquidationValue();
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int roundUpToNearestTen(double value) {
        return (int) (Math.ceil(Math.max(0, value) / 10.0) * 10);
    }
}
