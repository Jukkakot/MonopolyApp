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
        Property property = selection.property();
        if (property == null) {
            return true;
        }
        if (property.getOwnerPlayer() != from) {
            return false;
        }
        if (property instanceof StreetProperty streetProperty && streetProperty.getBuildingLevel() > 0) {
            return false;
        }
        return true;
    }

    private void applySelection(Player from, Player to, TradeSelection selection) {
        if (selection.moneyAmount() > 0) {
            to.giveMoney(from, selection.moneyAmount());
        }
        if (selection.property() != null) {
            from.transferPropertyTo(to, selection.property());
        }
        if (selection.jailCard()) {
            from.transferGetOutOfJailCardTo(to);
        }
    }
}
