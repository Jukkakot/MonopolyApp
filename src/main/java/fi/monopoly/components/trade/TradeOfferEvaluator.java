package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

public final class TradeOfferEvaluator {
    private static final int JAIL_CARD_VALUE = 60;

    public TradeDecision evaluateForRecipient(TradeOffer offer) {
        return evaluateForRecipient(offer, BotTradeProfile.BALANCED);
    }

    public TradeDecision evaluateForRecipient(TradeOffer offer, BotTradeProfile profile) {
        if (!offer.isValid()) {
            return new TradeDecision(false, -10_000, "Reject trade: invalid offer");
        }
        Player recipient = offer.recipient();
        double gainScore = selectionValue(recipient, offer.offeredToRecipient(), true);
        double lossScore = selectionValue(recipient, offer.requestedFromRecipient(), false);
        double score = gainScore - lossScore;
        boolean accept = score >= profile.acceptThreshold();
        return new TradeDecision(
                accept,
                score,
                accept
                        ? "Accept trade (" + profile.name() + "): receive value " + round(gainScore) + " vs give " + round(lossScore)
                        : "Reject trade (" + profile.name() + "): receive value " + round(gainScore) + " vs give " + round(lossScore)
        );
    }

    public TradeOffer proposeCounterOffer(TradeOffer offer, BotTradeProfile profile) {
        TradeDecision currentDecision = evaluateForRecipient(offer, profile);
        if (currentDecision.accept()) {
            return null;
        }
        int adjustment = Math.min(profile.maxCounterAdjustment(), roundUpToNearestTen(profile.acceptThreshold() - currentDecision.score()));
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
        return evaluateForRecipient(counterOffer, profile).accept() ? counterOffer : null;
    }

    private double selectionValue(Player perspective, TradeSelection selection, boolean receiving) {
        double value = selection.moneyAmount();
        if (selection.jailCard()) {
            value += JAIL_CARD_VALUE;
        }
        if (selection.property() != null) {
            value += propertyValue(perspective, selection.property(), receiving);
        }
        return value;
    }

    private double propertyValue(Player perspective, Property property, boolean receiving) {
        double value = property.getLiquidationValue();
        if (property.isMortgaged()) {
            value -= property.getMortgageInterest();
        }
        StreetType streetType = property.getSpotType().streetType;
        int ownedInSet = perspective.getOwnedProperties(streetType).size();
        if (receiving) {
            ownedInSet += 1;
        }
        int setSize = fi.monopoly.types.SpotType.getNumberOfSpots(streetType);
        if (streetType.placeType == PlaceType.STREET && ownedInSet >= setSize) {
            value += 220;
        } else if (ownedInSet > 0) {
            value += 70;
        }
        if (!receiving && perspective.ownsAllStreetProperties(streetType)) {
            value += 220;
        }
        if (streetType.placeType == PlaceType.RAILROAD) {
            value += ownedInSet * 35;
        }
        if (streetType.placeType == PlaceType.UTILITY && ownedInSet >= 2) {
            value += 50;
        }
        return value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private int roundUpToNearestTen(double value) {
        return (int) (Math.ceil(Math.max(0, value) / 10.0) * 10);
    }
}
