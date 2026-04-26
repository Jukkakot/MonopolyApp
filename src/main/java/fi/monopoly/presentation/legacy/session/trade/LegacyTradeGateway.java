package fi.monopoly.presentation.legacy.session.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.trade.*;
import fi.monopoly.domain.session.TradeOfferState;
import fi.monopoly.domain.session.TradeSelectionState;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@RequiredArgsConstructor
public final class LegacyTradeGateway implements fi.monopoly.application.session.trade.TradeGateway {
    private final Supplier<List<Player>> playersSupplier;
    private final TradeOfferEvaluator tradeOfferEvaluator;

    public LegacyTradeGateway(Supplier<List<Player>> playersSupplier) {
        this(playersSupplier, new TradeOfferEvaluator());
    }


    @Override
    public Player playerById(String playerId) {
        if (playerId == null) {
            return null;
        }
        for (Player player : playersSupplier.get()) {
            if (playerId.equals("player-" + player.getId())) {
                return player;
            }
        }
        return null;
    }

    @Override
    public TradeOffer toLegacyOffer(TradeOfferState offerState) {
        Player proposer = playerById(offerState.proposerPlayerId());
        Player recipient = playerById(offerState.recipientPlayerId());
        if (proposer == null || recipient == null) {
            return null;
        }
        return new TradeOffer(
                proposer,
                recipient,
                toLegacySelection(offerState.offeredToRecipient()),
                toLegacySelection(offerState.requestedFromRecipient())
        );
    }

    @Override
    public boolean isValidOffer(TradeOfferState offerState) {
        TradeOffer offer = toLegacyOffer(offerState);
        return offer != null && offer.isValid();
    }

    @Override
    public boolean applyOffer(TradeOfferState offerState) {
        TradeOffer offer = toLegacyOffer(offerState);
        return offer != null && offer.apply();
    }

    @Override
    public TradeDecision evaluateForRecipient(TradeOfferState offerState, BotTradeProfile profile, StrongBotConfig strongConfig) {
        TradeOffer offer = toLegacyOffer(offerState);
        return offer == null ? new TradeDecision(false, Double.NEGATIVE_INFINITY, "Trade offer could not be resolved") : tradeOfferEvaluator.evaluateForRecipient(offer, profile, strongConfig);
    }

    @Override
    public TradeOfferState proposeCounterOffer(TradeOfferState offerState, BotTradeProfile profile, StrongBotConfig strongConfig) {
        TradeOffer offer = toLegacyOffer(offerState);
        if (offer == null) {
            return null;
        }
        TradeOffer counterOffer = tradeOfferEvaluator.proposeCounterOffer(offer, profile, strongConfig);
        return counterOffer == null ? null : toState(counterOffer);
    }

    public TradeOfferState toState(TradeOffer offer) {
        return new TradeOfferState(
                playerId(offer.proposer()),
                playerId(offer.recipient()),
                toStateSelection(offer.offeredToRecipient()),
                toStateSelection(offer.requestedFromRecipient())
        );
    }

    private TradeSelection toLegacySelection(TradeSelectionState selectionState) {
        List<Property> properties = new ArrayList<>();
        for (String propertyId : selectionState.propertyIds()) {
            Property property = propertyById(propertyId);
            if (property != null) {
                properties.add(property);
            }
        }
        return new TradeSelection(selectionState.moneyAmount(), properties, selectionState.jailCardCount() > 0);
    }

    private TradeSelectionState toStateSelection(TradeSelection selection) {
        return new TradeSelectionState(
                selection.moneyAmount(),
                selection.properties().stream().map(property -> property.getSpotType().name()).toList(),
                selection.jailCard() ? 1 : 0
        );
    }

    public Property propertyById(String propertyId) {
        return propertyId == null ? null : PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }

    public String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }
}
