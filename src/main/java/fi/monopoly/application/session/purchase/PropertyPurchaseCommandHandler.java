package fi.monopoly.application.session.purchase;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.result.DomainEvent;
import fi.monopoly.application.session.auction.AuctionCommandHandler;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TurnContinuationState;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PropertyPurchaseCommandHandler {
    private final String sessionId;
    private final Supplier<SessionState> currentStateSupplier;
    private final Consumer<PendingDecision> pendingDecisionSetter;
    private final Consumer<AuctionState> auctionStateSetter;
    private final Consumer<TurnContinuationState> turnContinuationSetter;
    private final PropertyPurchaseGateway gateway;
    private final AuctionCommandHandler auctionCommandHandler;
    private PropertyPurchaseContext activeContext;

    public PropertyPurchaseCommandHandler(
            String sessionId,
            Supplier<SessionState> currentStateSupplier,
            Consumer<PendingDecision> pendingDecisionSetter,
            Consumer<AuctionState> auctionStateSetter,
            Consumer<TurnContinuationState> turnContinuationSetter,
            PropertyPurchaseGateway gateway,
            AuctionCommandHandler auctionCommandHandler
    ) {
        this.sessionId = sessionId;
        this.currentStateSupplier = currentStateSupplier;
        this.pendingDecisionSetter = pendingDecisionSetter;
        this.auctionStateSetter = auctionStateSetter;
        this.turnContinuationSetter = turnContinuationSetter;
        this.gateway = gateway;
        this.auctionCommandHandler = auctionCommandHandler;
    }

    public PendingDecision openDecision(
            Player player,
            Property property,
            String message,
            TurnContinuationState continuationState,
            CallbackAction onComplete
    ) {
        String propertyId = property.getSpotType().name();
        PendingDecision decision = new PendingDecision(
                "property-purchase:" + player.getId() + ":" + propertyId,
                DecisionType.PROPERTY_PURCHASE,
                playerId(player),
                List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY),
                message,
                new PropertyPurchaseDecisionPayload(propertyId, property.getDisplayName(), property.getPrice())
        );
        activeContext = new PropertyPurchaseContext(decision.decisionId(), player, property, continuationState, onComplete);
        auctionStateSetter.accept(null);
        turnContinuationSetter.accept(continuationState);
        pendingDecisionSetter.accept(decision);
        return decision;
    }

    public boolean supports(SessionCommand command) {
        return command instanceof BuyPropertyCommand || command instanceof DeclinePropertyCommand;
    }

    public CommandResult handle(SessionCommand command) {
        if (command instanceof BuyPropertyCommand buyPropertyCommand) {
            return handleBuy(buyPropertyCommand);
        }
        if (command instanceof DeclinePropertyCommand declinePropertyCommand) {
            return handleDecline(declinePropertyCommand);
        }
        return reject("UNSUPPORTED_COMMAND", "Unsupported property purchase command");
    }

    private CommandResult handleBuy(BuyPropertyCommand command) {
        PropertyPurchaseContext context = validate(command.sessionId(), command.actorPlayerId(), command.decisionId(), command.propertyId());
        if (context == null) {
            return reject("INVALID_PROPERTY_PURCHASE", "Property purchase command is not valid in the current state");
        }
        if (!gateway.buyProperty(context.player(), context.property())) {
            return reject("PROPERTY_CANNOT_BE_BOUGHT", "Property can no longer be bought");
        }
        pendingDecisionSetter.accept(null);
        auctionStateSetter.accept(null);
        turnContinuationSetter.accept(null);
        PropertyPurchaseContext resolvedContext = activeContext;
        activeContext = null;
        resolvedContext.onComplete().doAction();
        return new CommandResult(
                true,
                currentStateSupplier.get(),
                List.of(new DomainEvent("PropertyBought", playerId(context.player()), context.property().getDisplayName())),
                List.of(),
                List.of()
        );
    }

    private CommandResult handleDecline(DeclinePropertyCommand command) {
        PropertyPurchaseContext context = validate(command.sessionId(), command.actorPlayerId(), command.decisionId(), command.propertyId());
        if (context == null) {
            return reject("INVALID_PROPERTY_PURCHASE", "Property decline command is not valid in the current state");
        }
        pendingDecisionSetter.accept(null);
        auctionCommandHandler.startAuction(context.player(), context.property(), context.continuationState(), context.onComplete());
        activeContext = null;
        return new CommandResult(
                true,
                currentStateSupplier.get(),
                List.of(
                        new DomainEvent("PropertyDeclined", playerId(context.player()), context.property().getDisplayName()),
                        new DomainEvent("AuctionStarted", playerId(context.player()), context.property().getDisplayName())
                ),
                List.of(),
                List.of()
        );
    }

    private PropertyPurchaseContext validate(String commandSessionId, String actorPlayerId, String decisionId, String propertyId) {
        if (!Objects.equals(sessionId, commandSessionId) || activeContext == null) {
            return null;
        }
        SessionState currentState = currentStateSupplier.get();
        PendingDecision pendingDecision = currentState.pendingDecision();
        if (pendingDecision == null || pendingDecision.decisionType() != DecisionType.PROPERTY_PURCHASE) {
            return null;
        }
        if (!Objects.equals(pendingDecision.actorPlayerId(), actorPlayerId)
                || !Objects.equals(pendingDecision.decisionId(), decisionId)) {
            return null;
        }
        if (!(pendingDecision.payload() instanceof PropertyPurchaseDecisionPayload payload)
                || !Objects.equals(payload.propertyId(), propertyId)) {
            return null;
        }
        if (!Objects.equals(activeContext.decisionId(), decisionId)
                || !Objects.equals(activeContext.property().getSpotType().name(), propertyId)
                || !Objects.equals(playerId(activeContext.player()), actorPlayerId)) {
            return null;
        }
        return activeContext;
    }

    private CommandResult reject(String code, String message) {
        return new CommandResult(false, currentStateSupplier.get(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }

    private record PropertyPurchaseContext(
            String decisionId,
            Player player,
            Property property,
            TurnContinuationState continuationState,
            CallbackAction onComplete
    ) {
    }
}
