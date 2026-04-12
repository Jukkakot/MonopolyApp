package fi.monopoly.application.session;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.application.command.SessionCommand;
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
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;

import java.util.List;
import java.util.function.Supplier;

public final class SessionApplicationService {
    private final String sessionId;
    private final Supplier<SessionState> sessionStateSupplier;
    private PendingDecision pendingDecisionOverride;
    private AuctionState auctionStateOverride;
    private DebtStateModel activeDebtOverride;
    private PropertyPurchaseCommandHandler propertyPurchaseCommandHandler;
    private RentAndDebtOpeningHandler rentAndDebtOpeningHandler;

    public SessionApplicationService(String sessionId, Supplier<SessionState> sessionStateSupplier) {
        this.sessionId = sessionId;
        this.sessionStateSupplier = sessionStateSupplier;
    }

    public SessionState currentState() {
        SessionState baseState = sessionStateSupplier.get();
        PendingDecision pendingDecision = pendingDecisionOverride != null ? pendingDecisionOverride : baseState.pendingDecision();
        AuctionState auctionState = auctionStateOverride != null ? auctionStateOverride : baseState.auctionState();
        DebtStateModel activeDebt = activeDebtOverride != null ? activeDebtOverride : baseState.activeDebt();
        TurnState turnState = baseState.turn();
        if (activeDebt != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.RESOLVING_DEBT, false, false);
        } else if (auctionState != null) {
            turnState = new TurnState(turnState.activePlayerId(), TurnPhase.WAITING_FOR_AUCTION, false, false);
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
                baseState.winnerPlayerId()
        );
    }

    public void configurePropertyPurchaseFlow(PopupService popupService, Players players) {
        propertyPurchaseCommandHandler = new PropertyPurchaseCommandHandler(
                sessionId,
                this::currentState,
                this::setPendingDecisionOverride,
                this::setAuctionStateOverride,
                new LegacyPropertyPurchaseGateway(popupService, players)
        );
    }

    public void configureRentAndDebtFlow(DebtController debtController) {
        rentAndDebtOpeningHandler = new RentAndDebtOpeningHandler(this::setActiveDebtOverride, new LegacyPaymentGateway(debtController));
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

    public CommandResult handle(SessionCommand command) {
        if (propertyPurchaseCommandHandler != null
                && (command instanceof BuyPropertyCommand || command instanceof DeclinePropertyCommand)) {
            return propertyPurchaseCommandHandler.handle(command);
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
}
