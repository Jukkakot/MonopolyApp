package fi.monopoly.application.session.trade;

import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.trade.BotTradeProfile;
import fi.monopoly.components.trade.TradeDecision;
import fi.monopoly.components.trade.TradeOffer;
import fi.monopoly.domain.session.TradeOfferState;

public interface TradeGateway {
    boolean playerExists(String playerId);

    TradeOffer toLegacyOffer(TradeOfferState offerState);

    boolean isValidOffer(TradeOfferState offerState);

    boolean applyOffer(TradeOfferState offerState);

    TradeDecision evaluateForRecipient(TradeOfferState offerState, BotTradeProfile profile, StrongBotConfig strongConfig);

    TradeOfferState proposeCounterOffer(TradeOfferState offerState, BotTradeProfile profile, StrongBotConfig strongConfig);
}
