package fi.monopoly.application.session;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclareBankruptcyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.AcceptTradeCommand;
import fi.monopoly.application.command.CancelTradeCommand;
import fi.monopoly.application.command.MortgagePropertyForDebtCommand;
import fi.monopoly.application.command.PayDebtCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.command.CounterTradeCommand;
import fi.monopoly.application.command.DeclineTradeCommand;
import fi.monopoly.application.command.EditTradeOfferCommand;
import fi.monopoly.application.command.OpenTradeCommand;
import fi.monopoly.application.command.SellBuildingForDebtCommand;
import fi.monopoly.application.command.SellBuildingRoundsAcrossSetForDebtCommand;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.command.SubmitTradeOfferCommand;
import fi.monopoly.application.session.auction.AuctionCommandHandler;
import fi.monopoly.application.session.debt.DebtRemediationCommandHandler;
import fi.monopoly.application.session.debt.RentAndDebtOpeningHandler;
import fi.monopoly.application.session.purchase.PropertyPurchaseCommandHandler;
import fi.monopoly.application.session.trade.TradeCommandHandler;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TradeState;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.presentation.session.auction.LegacyAuctionGateway;
import fi.monopoly.presentation.session.debt.LegacyDebtRemediationGateway;
import fi.monopoly.presentation.session.debt.LegacyPaymentGateway;
import fi.monopoly.presentation.session.purchase.LegacyPropertyPurchaseGateway;
import fi.monopoly.presentation.session.trade.LegacyTradeGateway;

import java.util.List;
import java.util.function.Supplier;

public final class SessionApplicationService {
    private final String sessionId;
    private final Supplier<SessionState> sessionStateSupplier;
    private PendingDecision pendingDecisionOverride;
    private AuctionState auctionStateOverride;
    private DebtStateModel activeDebtOverride;
    private TradeState tradeStateOverride;
    private AuctionCommandHandler auctionCommandHandler;
    private PropertyPurchaseCommandHandler propertyPurchaseCommandHandler;
    private RentAndDebtOpeningHandler rentAndDebtOpeningHandler;
    private DebtRemediationCommandHandler debtRemediationCommandHandler;
    private TradeCommandHandler tradeCommandHandler;

    public SessionApplicationService(String sessionId, Supplier<SessionState> sessionStateSupplier) {
        this.sessionId = sessionId;
        this.sessionStateSupplier = sessionStateSupplier;
    }

    public SessionState currentState() {
        SessionState baseState = sessionStateSupplier.get();
        PendingDecision pendingDecision = pendingDecisionOverride != null ? pendingDecisionOverride : baseState.pendingDecision();
        AuctionState auctionState = auctionStateOverride != null ? auctionStateOverride : baseState.auctionState();
        DebtStateModel activeDebt = activeDebtOverride != null ? activeDebtOverride : baseState.activeDebt();
        TradeState tradeState = tradeStateOverride != null ? tradeStateOverride : baseState.tradeState();
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
        return new SessionState(
                baseState.sessionId(),
                baseState.version(),
                baseState.status(),
                baseState.seats(),
                baseState.players(),
                turnState,
                pendingDecision,
                auctionState,
                activeDebt,
                tradeState,
                baseState.winnerPlayerId()
        );
    }

    public void configureAuctionFlow(PopupService popupService, Players players) {
        auctionCommandHandler = new AuctionCommandHandler(
                sessionId,
                this::currentState,
                this::setAuctionStateOverride,
                new LegacyAuctionGateway(popupService, players)
        );
    }

    public void configurePropertyPurchaseFlow(PopupService popupService, Players players) {
        if (auctionCommandHandler == null) {
            configureAuctionFlow(popupService, players);
        }
        propertyPurchaseCommandHandler = new PropertyPurchaseCommandHandler(
                sessionId,
                this::currentState,
                this::setPendingDecisionOverride,
                this::setAuctionStateOverride,
                new LegacyPropertyPurchaseGateway(popupService, players),
                auctionCommandHandler
        );
    }

    public void configureRentAndDebtFlow(DebtController debtController) {
        rentAndDebtOpeningHandler = new RentAndDebtOpeningHandler(this::setActiveDebtOverride, new LegacyPaymentGateway(debtController));
        debtRemediationCommandHandler = new DebtRemediationCommandHandler(
                sessionId,
                this::currentState,
                this::setActiveDebtOverride,
                new LegacyDebtRemediationGateway(debtController)
        );
    }

    public void configureTradeFlow(Supplier<List<Player>> playersSupplier) {
        tradeCommandHandler = new TradeCommandHandler(
                sessionId,
                this::currentState,
                this::setTradeStateOverride,
                new LegacyTradeGateway(playersSupplier)
        );
    }

    public PendingDecision openPropertyPurchaseDecision(Player player, Property property, String message, CallbackAction onComplete) {
        if (propertyPurchaseCommandHandler == null) {
            throw new IllegalStateException("Property purchase flow has not been configured");
        }
        return propertyPurchaseCommandHandler.openDecision(player, property, message, onComplete);
    }

    public void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        if (rentAndDebtOpeningHandler == null) {
            throw new IllegalStateException("Rent and debt flow has not been configured");
        }
        rentAndDebtOpeningHandler.handle(request, onResolved);
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

    public CommandResult handleComputerAuctionAction(String actorPlayerId) {
        if (auctionCommandHandler == null) {
            return rejected("AUCTION_NOT_CONFIGURED", "Auction flow has not been configured");
        }
        return auctionCommandHandler.handleComputerAction(actorPlayerId);
    }

    public CommandResult handle(SessionCommand command) {
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
}
