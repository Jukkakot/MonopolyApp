package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;

public record TradeOffer(
        Player proposer,
        Player recipient,
        TradeSelection offeredToRecipient,
        TradeSelection requestedFromRecipient
) {
    public TradeOffer withOfferedToRecipient(TradeSelection selection) {
        return new TradeOffer(proposer, recipient, selection, requestedFromRecipient);
    }

    public TradeOffer withRequestedFromRecipient(TradeSelection selection) {
        return new TradeOffer(proposer, recipient, offeredToRecipient, selection);
    }

    public TradeOffer reversePerspective() {
        return new TradeOffer(recipient, proposer, requestedFromRecipient, offeredToRecipient);
    }

    public boolean isEmpty() {
        return offeredToRecipient.isEmpty() && requestedFromRecipient.isEmpty();
    }

    public boolean isValid() {
        if (proposer == null || recipient == null || proposer == recipient) {
            return false;
        }
        if (isEmpty()) {
            return false;
        }
        return canSendSelection(proposer, offeredToRecipient) && canSendSelection(recipient, requestedFromRecipient);
    }

    public boolean apply() {
        if (!isValid()) {
            return false;
        }
        applySelection(proposer, recipient, offeredToRecipient);
        applySelection(recipient, proposer, requestedFromRecipient);
        return true;
    }

    private boolean canSendSelection(Player from, TradeSelection selection) {
        if (selection.moneyAmount() < 0) {
            return false;
        }
        if (selection.moneyAmount() > from.getMoneyAmount()) {
            return false;
        }
        if (selection.jailCard() && !from.hasGetOutOfJailCard()) {
            return false;
        }
        for (Property property : selection.properties()) {
            if (property.getOwnerPlayer() != from) {
                return false;
            }
            if (property instanceof StreetProperty streetProperty && streetProperty.getBuildingLevel() > 0) {
                return false;
            }
        }
        return true;
    }

    private void applySelection(Player from, Player to, TradeSelection selection) {
        if (selection.moneyAmount() > 0) {
            to.giveMoney(from, selection.moneyAmount());
        }
        for (Property property : selection.properties()) {
            from.transferPropertyTo(to, property);
        }
        if (selection.jailCard()) {
            from.transferGetOutOfJailCardTo(to);
        }
    }
}
