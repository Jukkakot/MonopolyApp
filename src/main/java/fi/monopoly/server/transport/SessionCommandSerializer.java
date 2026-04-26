package fi.monopoly.server.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fi.monopoly.application.command.*;

import java.io.IOException;

/**
 * Serializes typed {@link SessionCommand} instances to JSON with a {@code "type"} discriminator
 * field, complementing {@link SessionCommandMapper} which does the reverse.
 */
public final class SessionCommandSerializer {

    private final ObjectMapper objectMapper;

    public SessionCommandSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(SessionCommand command) throws IOException {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("sessionId", command.sessionId());

        if (command instanceof RollDiceCommand c) {
            node.put("type", "RollDice");
            node.put("actorPlayerId", c.actorPlayerId());
        } else if (command instanceof EndTurnCommand c) {
            node.put("type", "EndTurn");
            node.put("actorPlayerId", c.actorPlayerId());
        } else if (command instanceof BuyPropertyCommand c) {
            node.put("type", "BuyProperty");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("decisionId", c.decisionId());
            node.put("propertyId", c.propertyId());
        } else if (command instanceof DeclinePropertyCommand c) {
            node.put("type", "DeclineProperty");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("decisionId", c.decisionId());
            node.put("propertyId", c.propertyId());
        } else if (command instanceof PayDebtCommand c) {
            node.put("type", "PayDebt");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("debtId", c.debtId());
        } else if (command instanceof MortgagePropertyForDebtCommand c) {
            node.put("type", "MortgagePropertyForDebt");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("debtId", c.debtId());
            node.put("propertyId", c.propertyId());
        } else if (command instanceof SellBuildingForDebtCommand c) {
            node.put("type", "SellBuildingForDebt");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("debtId", c.debtId());
            node.put("propertyId", c.propertyId());
            node.put("count", c.count());
        } else if (command instanceof DeclareBankruptcyCommand c) {
            node.put("type", "DeclareBankruptcy");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("debtId", c.debtId());
        } else if (command instanceof SellBuildingRoundsAcrossSetForDebtCommand c) {
            node.put("type", "SellBuildingRoundsAcrossSetForDebt");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("debtId", c.debtId());
            node.put("propertyId", c.propertyId());
            node.put("rounds", c.rounds());
        } else if (command instanceof PassAuctionCommand c) {
            node.put("type", "PassAuction");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("auctionId", c.auctionId());
        } else if (command instanceof PlaceAuctionBidCommand c) {
            node.put("type", "PlaceAuctionBid");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("auctionId", c.auctionId());
            node.put("amount", c.amount());
        } else if (command instanceof FinishAuctionResolutionCommand c) {
            node.put("type", "FinishAuctionResolution");
            node.put("auctionId", c.auctionId());
        } else if (command instanceof AcceptTradeCommand c) {
            node.put("type", "AcceptTrade");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
        } else if (command instanceof EditTradeOfferCommand c) {
            node.put("type", "EditTradeOffer");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
            node.set("patch", objectMapper.valueToTree(c.patch()));
        } else if (command instanceof OpenTradeCommand c) {
            node.put("type", "OpenTrade");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("recipientPlayerId", c.recipientPlayerId());
        } else if (command instanceof SubmitTradeOfferCommand c) {
            node.put("type", "SubmitTradeOffer");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
        } else if (command instanceof CancelTradeCommand c) {
            node.put("type", "CancelTrade");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
        } else if (command instanceof CounterTradeCommand c) {
            node.put("type", "CounterTrade");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
        } else if (command instanceof DeclineTradeCommand c) {
            node.put("type", "DeclineTrade");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("tradeId", c.tradeId());
        } else if (command instanceof BuyBuildingRoundCommand c) {
            node.put("type", "BuyBuildingRound");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("propertyId", c.propertyId());
        } else if (command instanceof ToggleMortgageCommand c) {
            node.put("type", "ToggleMortgage");
            node.put("actorPlayerId", c.actorPlayerId());
            node.put("propertyId", c.propertyId());
        } else if (command instanceof RefreshSessionViewCommand) {
            node.put("type", "RefreshSessionView");
        } else {
            throw new IllegalArgumentException(
                    "Unknown SessionCommand type: " + command.getClass().getName());
        }
        return objectMapper.writeValueAsString(node);
    }
}
