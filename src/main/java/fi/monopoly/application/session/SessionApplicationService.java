package fi.monopoly.application.session;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.auction.AuctionCommandHandler;
import fi.monopoly.application.session.auction.AuctionGateway;
import fi.monopoly.application.session.debt.DebtOpeningGateway;
import fi.monopoly.application.session.debt.DebtRemediationCommandHandler;
import fi.monopoly.application.session.debt.DebtRemediationGateway;
import fi.monopoly.application.session.debt.RentAndDebtOpeningHandler;
import fi.monopoly.application.session.purchase.PropertyPurchaseCommandHandler;
import fi.monopoly.application.session.purchase.PropertyPurchaseGateway;
import fi.monopoly.application.session.trade.TradeCommandHandler;
import fi.monopoly.application.session.trade.TradeGateway;
import fi.monopoly.application.session.turn.TurnActionCommandHandler;
import fi.monopoly.application.session.turn.TurnActionGateway;
import fi.monopoly.application.session.turn.TurnContinuationGateway;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Supplier;

/**
 * Application-layer entry point for command handling around a single Monopoly session.
 *
 * <p>This service exposes the projected authoritative {@link SessionState}, routes incoming
 * session commands to the relevant subsystem handlers, and keeps temporary overrides for flows
 * that are already represented in the separated session model but still need to coordinate with
 * legacy runtime objects.</p>
 */
@RequiredArgsConstructor
public final class SessionApplicationService implements SessionCommandPort, SessionPresentationStatePort, SessionPaymentPort {
    private final String sessionId;
    private final Supplier<SessionState> sessionStateSupplier;
    private PendingDecision pendingDecisionOverride;
    private AuctionState auctionStateOverride;
    private DebtStateModel activeDebtOverride;
    private TradeState tradeStateOverride;
    private TurnContinuationState turnContinuationOverride;
    private AuctionCommandHandler auctionCommandHandler;
    private PropertyPurchaseCommandHandler propertyPurchaseCommandHandler;
    private RentAndDebtOpeningHandler rentAndDebtOpeningHandler;
    private DebtRemediationCommandHandler debtRemediationCommandHandler;
    private TradeCommandHandler tradeCommandHandler;
    private TurnActionCommandHandler turnActionCommandHandler;
    private TurnContinuationGateway turnContinuationGateway;

    public SessionState currentState() {
        SessionState baseState = sessionStateSupplier.get();
        PendingDecision pendingDecision = pendingDecisionOverride != null ? pendingDecisionOverride : baseState.pendingDecision();
        AuctionState auctionState = auctionStateOverride != null ? auctionStateOverride : baseState.auctionState();
        DebtStateModel activeDebt = activeDebtOverride != null ? activeDebtOverride : baseState.activeDebt();
        TradeState tradeState = tradeStateOverride != null ? tradeStateOverride : baseState.tradeState();
        TurnContinuationState turnContinuationState = turnContinuationOverride != null ? turnContinuationOverride : baseState.turnContinuationState();
        if (shouldClearStalePendingDecisionOverride(baseState, auctionState, activeDebt, tradeState)) {
            pendingDecisionOverride = null;
            pendingDecision = null;
        }
        TurnState turnState = baseState.turn();
        if (activeDebt != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.RESOLVING_DEBT, false, false);
        } else if (auctionState != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.WAITING_FOR_AUCTION, false, false);
        } else if (tradeState != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.WAITING_FOR_DECISION, false, false);
        } else if (pendingDecision != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.WAITING_FOR_DECISION, false, false);
        }
        return baseState.toBuilder()
                .turn(turnState)
                .pendingDecision(pendingDecision)
                .auctionState(auctionState)
                .activeDebt(activeDebt)
                .tradeState(tradeState)
                .turnContinuationState(turnContinuationState)
                .build();
    }

    public void configureAuctionFlow(AuctionGateway gateway) {
        auctionCommandHandler = new AuctionCommandHandler(
                sessionId,
                this::currentState,
                this::setAuctionStateOverride,
                this::setTurnContinuationOverride,
                this::resumeContinuation,
                gateway
        );
    }

    public void configurePropertyPurchaseFlow(PropertyPurchaseGateway gateway) {
        if (auctionCommandHandler == null) {
            throw new IllegalStateException("Auction flow must be configured before property purchase flow");
        }
        propertyPurchaseCommandHandler = new PropertyPurchaseCommandHandler(
                sessionId,
                this::currentState,
                this::setPendingDecisionOverride,
                this::setAuctionStateOverride,
                this::setTurnContinuationOverride,
                this::resumeContinuation,
                gateway,
                auctionCommandHandler
        );
    }

    public void configureRentAndDebtFlow(
            DebtOpeningGateway debtOpeningGateway,
            DebtRemediationGateway debtRemediationGateway
    ) {
        rentAndDebtOpeningHandler = new RentAndDebtOpeningHandler(
                this::setActiveDebtOverride,
                this::setTurnContinuationOverride,
                this::resumeContinuation,
                debtOpeningGateway
        );
        debtRemediationCommandHandler = new DebtRemediationCommandHandler(
                sessionId,
                this::currentState,
                this::setActiveDebtOverride,
                this::setTurnContinuationOverride,
                debtRemediationGateway
        );
    }

    public void configureTradeFlow(TradeGateway gateway) {
        tradeCommandHandler = new TradeCommandHandler(
                sessionId,
                this::currentState,
                this::setTradeStateOverride,
                gateway
        );
    }

    public void configureTurnActionFlow(TurnActionGateway gateway) {
        turnActionCommandHandler = new TurnActionCommandHandler(
                sessionId,
                this::currentState,
                gateway
        );
    }

    public void configureTurnContinuationFlow(TurnContinuationGateway gateway) {
        this.turnContinuationGateway = gateway;
    }

    public PendingDecision openPropertyPurchaseDecision(
            Player player,
            Property property,
            String message,
            TurnContinuationState continuationState
    ) {
        if (propertyPurchaseCommandHandler == null) {
            throw new IllegalStateException("Property purchase flow has not been configured");
        }
        return propertyPurchaseCommandHandler.openDecision(player, property, message, continuationState);
    }

    public void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        if (rentAndDebtOpeningHandler == null) {
            throw new IllegalStateException("Rent and debt flow has not been configured");
        }
        rentAndDebtOpeningHandler.handle(request, continuationState, onResolved);
    }

    public void clearActiveDebtOverride() {
        activeDebtOverride = null;
    }

    public boolean hasActiveAuction() {
        return currentState().auctionState() != null;
    }

    public boolean hasActiveTrade() {
        return currentState().tradeState() != null;
    }

    public boolean hasAuctionOverride() {
        return auctionStateOverride != null;
    }

    public boolean hasTradeOverride() {
        return tradeStateOverride != null;
    }

    public boolean hasPendingDecisionOverride() {
        return pendingDecisionOverride != null;
    }

    public void restoreFrom(SessionState restoredState) {
        if (restoredState == null) {
            pendingDecisionOverride = null;
            auctionStateOverride = null;
            activeDebtOverride = null;
            tradeStateOverride = null;
            turnContinuationOverride = null;
            return;
        }
        pendingDecisionOverride = restoredState.pendingDecision();
        auctionStateOverride = restoredState.auctionState();
        activeDebtOverride = restoredState.activeDebt();
        tradeStateOverride = restoredState.tradeState();
        turnContinuationOverride = restoredState.turnContinuationState();
    }

    public void setTurnContinuationOverride(TurnContinuationState turnContinuationState) {
        this.turnContinuationOverride = turnContinuationState;
    }

    private void resumeContinuation(TurnContinuationState continuationState) {
        if (continuationState == null || turnContinuationGateway == null) {
            return;
        }
        turnContinuationGateway.resume(continuationState);
    }

    public CommandResult handleComputerAuctionAction(String actorPlayerId) {
        if (auctionCommandHandler == null) {
            return rejected("AUCTION_NOT_CONFIGURED", "Auction flow has not been configured");
        }
        return auctionCommandHandler.handleComputerAction(actorPlayerId);
    }

    public CommandResult handle(SessionCommand command) {
        return dispatch(command);
    }

    private CommandResult dispatch(SessionCommand command) {
        if (propertyPurchaseCommandHandler != null
                && (command instanceof BuyPropertyCommand || command instanceof DeclinePropertyCommand)) {
            return propertyPurchaseCommandHandler.handle(command);
        }
        if (auctionCommandHandler != null
                && (command instanceof PlaceAuctionBidCommand
                || command instanceof PassAuctionCommand
                || command instanceof FinishAuctionResolutionCommand)) {
            return auctionCommandHandler.handle(command);
        }
        if (debtRemediationCommandHandler != null
                && (command instanceof PayDebtCommand
                || command instanceof MortgagePropertyForDebtCommand
                || command instanceof SellBuildingForDebtCommand
                || command instanceof SellBuildingRoundsAcrossSetForDebtCommand
                || command instanceof DeclareBankruptcyCommand)) {
            return debtRemediationCommandHandler.handle(command);
        }
        if (tradeCommandHandler != null
                && (command instanceof OpenTradeCommand
                || command instanceof EditTradeOfferCommand
                || command instanceof SubmitTradeOfferCommand
                || command instanceof AcceptTradeCommand
                || command instanceof DeclineTradeCommand
                || command instanceof CounterTradeCommand
                || command instanceof CancelTradeCommand)) {
            return tradeCommandHandler.handle(command);
        }
        if (turnActionCommandHandler != null
                && (command instanceof RollDiceCommand
                || command instanceof EndTurnCommand
                || command instanceof BuyBuildingRoundCommand
                || command instanceof ToggleMortgageCommand)) {
            return turnActionCommandHandler.handle(command);
        }
        if (command instanceof RefreshSessionViewCommand refreshSessionViewCommand) {
            if (!sessionId.equals(refreshSessionViewCommand.sessionId())) {
                return rejected("WRONG_SESSION", "Command session does not match active session");
            }
            return accepted();
        }
        return rejected("UNSUPPORTED_COMMAND", "Command is not supported by the PR1 seam");
    }

    private CommandResult accepted() {
        return new CommandResult(true, currentState(), List.of(), List.of(), List.of());
    }

    private CommandResult rejected(String code, String message) {
        return new CommandResult(false, currentState(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }

    private void setPendingDecisionOverride(PendingDecision pendingDecision) {
        this.pendingDecisionOverride = pendingDecision;
    }

    private void setAuctionStateOverride(AuctionState auctionState) {
        this.auctionStateOverride = auctionState;
    }

    private void setActiveDebtOverride(DebtStateModel activeDebt) {
        this.activeDebtOverride = activeDebt;
    }

    private void setTradeStateOverride(TradeState tradeState) {
        this.tradeStateOverride = tradeState;
    }

    private boolean shouldClearStalePendingDecisionOverride(
            SessionState baseState,
            AuctionState auctionState,
            DebtStateModel activeDebt,
            TradeState tradeState
    ) {
        if (pendingDecisionOverride == null
                || baseState.pendingDecision() != null
                || auctionState != null
                || activeDebt != null
                || tradeState != null) {
            return false;
        }
        if (!(pendingDecisionOverride.payload() instanceof PropertyPurchaseDecisionPayload payload)) {
            return false;
        }
        Property property = PropertyFactory.getProperty(SpotType.valueOf(payload.propertyId()));
        return property == null || property.getOwnerPlayer() != null;
    }
}
