package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

public final class TradeOfferEvaluator {
    private static final int JAIL_CARD_VALUE = 60;
    private static final int ACCEPT_THRESHOLD = 0;

    public TradeDecision evaluateForRecipient(TradeOffer offer) {
        if (!offer.isValid()) {
            return new TradeDecision(false, -10_000, "Reject trade: invalid offer");
        }
        Player recipient = offer.recipient();
        double gainScore = selectionValue(recipient, offer.offeredToRecipient(), true);
        double lossScore = selectionValue(recipient, offer.requestedFromRecipient(), false);
        double score = gainScore - lossScore;
        boolean accept = score >= ACCEPT_THRESHOLD;
        return new TradeDecision(
                accept,
                score,
                accept
                        ? "Accept trade: receive value " + round(gainScore) + " vs give " + round(lossScore)
                        : "Reject trade: receive value " + round(gainScore) + " vs give " + round(lossScore)
        );
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
}
