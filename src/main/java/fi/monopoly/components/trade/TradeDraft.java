package fi.monopoly.components.trade;

import fi.monopoly.components.Player;

public record TradeDraft(
        Player proposer,
        Player recipient,
        TradeSelection offeredToRecipient,
        TradeSelection requestedFromRecipient
) {
    public TradeDraft withOfferedToRecipient(TradeSelection selection) {
        return new TradeDraft(proposer, recipient, selection, requestedFromRecipient);
    }

    public TradeDraft withRequestedFromRecipient(TradeSelection selection) {
        return new TradeDraft(proposer, recipient, offeredToRecipient, selection);
    }

    public TradeOffer toOffer() {
        return new TradeOffer(proposer, recipient, offeredToRecipient, requestedFromRecipient);
    }

    public TradeDraft asCounterOffer() {
        return new TradeDraft(recipient, proposer, requestedFromRecipient, offeredToRecipient);
    }
}
