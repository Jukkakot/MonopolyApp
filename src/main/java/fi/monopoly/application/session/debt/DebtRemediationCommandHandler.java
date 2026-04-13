package fi.monopoly.application.session.debt;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.result.DomainEvent;
import fi.monopoly.presentation.session.debt.LegacyDebtRemediationGateway;
import fi.monopoly.domain.session.DebtAction;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.PaymentObligation;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class DebtRemediationCommandHandler {
    private final String sessionId;
    private final Supplier<SessionState> sessionStateSupplier;
    private final LegacyDebtRemediationGateway gateway;
    private final Consumer<DebtStateModel> activeDebtUpdater;

    public DebtRemediationCommandHandler(
            String sessionId,
            Supplier<SessionState> sessionStateSupplier,
            Consumer<DebtStateModel> activeDebtUpdater,
            LegacyDebtRemediationGateway gateway
    ) {
        this.sessionId = sessionId;
        this.sessionStateSupplier = sessionStateSupplier;
        this.activeDebtUpdater = activeDebtUpdater;
        this.gateway = gateway;
    }

    public CommandResult handle(SessionCommand command) {
        if (!(command instanceof PayDebtCommand
                || command instanceof MortgagePropertyForDebtCommand
                || command instanceof SellBuildingForDebtCommand
                || command instanceof SellBuildingRoundsAcrossSetForDebtCommand
                || command instanceof DeclareBankruptcyCommand)) {
            return unsupported();
        }

        SessionState state = sessionStateSupplier.get();
        DebtStateModel debt = state.activeDebt();
        if (debt == null) {
            return rejected("NO_ACTIVE_DEBT", "No active debt to remediate");
        }

        if (command instanceof PayDebtCommand payDebtCommand) {
            if (!validBase(payDebtCommand.sessionId(), payDebtCommand.actorPlayerId(), payDebtCommand.debtId(), debt)) {
                return rejected("INVALID_DEBT_ACTION", "Debt action does not match the active debt");
            }
            if (debt.currentCash() < debt.amountRemaining()) {
                return rejected("DEBT_NOT_PAYABLE", "Current cash does not cover the debt");
            }
            gateway.payDebtNow();
            return accepted("DebtResolved");
        }

        if (command instanceof MortgagePropertyForDebtCommand mortgageCommand) {
            if (!validBase(mortgageCommand.sessionId(), mortgageCommand.actorPlayerId(), mortgageCommand.debtId(), debt)) {
                return rejected("INVALID_DEBT_ACTION", "Debt action does not match the active debt");
            }
            Property property = gateway.propertyById(mortgageCommand.propertyId());
            if (!isOwnedByDebtor(property, debt.debtorPlayerId()) || property.isMortgaged() || !canMortgage(property)) {
                return rejected("INVALID_MORTGAGE", "Property cannot be mortgaged for the active debt");
            }
            if (!gateway.mortgageProperty(mortgageCommand.propertyId())) {
                return rejected("MORTGAGE_FAILED", "Property mortgage failed");
            }
            refreshDebtState(debt);
            return accepted("PropertyMortgaged");
        }

        if (command instanceof SellBuildingForDebtCommand sellCommand) {
            if (!validBase(sellCommand.sessionId(), sellCommand.actorPlayerId(), sellCommand.debtId(), debt)) {
                return rejected("INVALID_DEBT_ACTION", "Debt action does not match the active debt");
            }
            Property property = gateway.propertyById(sellCommand.propertyId());
            if (!(property instanceof StreetProperty streetProperty)
                    || !isOwnedByDebtor(property, debt.debtorPlayerId())
                    || !streetProperty.canSellHouses(sellCommand.count())) {
                return rejected("INVALID_BUILDING_SALE", "Buildings cannot be sold for the active debt");
            }
            if (!gateway.sellBuildings(sellCommand.propertyId(), sellCommand.count())) {
                return rejected("BUILDING_SALE_FAILED", "Building sale failed");
            }
            refreshDebtState(debt);
            return accepted("BuildingSold");
        }

        if (command instanceof SellBuildingRoundsAcrossSetForDebtCommand sellRoundsCommand) {
            if (!validBase(sellRoundsCommand.sessionId(), sellRoundsCommand.actorPlayerId(), sellRoundsCommand.debtId(), debt)) {
                return rejected("INVALID_DEBT_ACTION", "Debt action does not match the active debt");
            }
            Property property = gateway.propertyById(sellRoundsCommand.propertyId());
            if (!(property instanceof StreetProperty streetProperty)
                    || !isOwnedByDebtor(property, debt.debtorPlayerId())
                    || !streetProperty.canSellBuildingRoundsAcrossSet(sellRoundsCommand.rounds())) {
                return rejected("INVALID_SET_BUILDING_SALE", "Building rounds cannot be sold for the active debt");
            }
            if (!gateway.sellBuildingRoundsAcrossSet(sellRoundsCommand.propertyId(), sellRoundsCommand.rounds())) {
                return rejected("SET_BUILDING_SALE_FAILED", "Building round sale failed");
            }
            refreshDebtState(debt);
            return accepted("BuildingRoundsSold");
        }

        DeclareBankruptcyCommand declareBankruptcyCommand = (DeclareBankruptcyCommand) command;
        if (!validBase(declareBankruptcyCommand.sessionId(), declareBankruptcyCommand.actorPlayerId(), declareBankruptcyCommand.debtId(), debt)) {
            return rejected("INVALID_DEBT_ACTION", "Debt action does not match the active debt");
        }
        if (!debt.bankruptcyRisk()) {
            return rejected("BANKRUPTCY_NOT_ALLOWED", "Debt can still be covered by available assets");
        }
        activeDebtUpdater.accept(null);
        gateway.declareBankruptcy();
        return accepted("BankruptcyDeclared");
    }

    private boolean validBase(String commandSessionId, String actorPlayerId, String debtId, DebtStateModel debt) {
        return sessionId.equals(commandSessionId)
                && debt.debtorPlayerId().equals(actorPlayerId)
                && debt.debtId().equals(debtId);
    }

    private boolean isOwnedByDebtor(Property property, String debtorPlayerId) {
        return property.getOwnerPlayer() != null && playerId(property.getOwnerPlayer()).equals(debtorPlayerId);
    }

    private boolean canMortgage(Property property) {
        if (property instanceof StreetProperty streetProperty) {
            return !streetProperty.getOwnerPlayer().getOwnedStreetProperties(property.getSpotType().streetType)
                    .stream()
                    .anyMatch(StreetProperty::hasBuildings);
        }
        return true;
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }

    private void refreshDebtState(DebtStateModel debt) {
        var legacyDebt = gateway.activeDebtState();
        if (legacyDebt == null || legacyDebt.paymentRequest() == null) {
            activeDebtUpdater.accept(null);
            return;
        }
        var request = legacyDebt.paymentRequest();
        int currentCash = request.debtor().getMoneyAmount();
        int liquidationValue = request.debtor().getTotalLiquidationValue();
        boolean bankruptcyRisk = currentCash + liquidationValue < debt.amountRemaining();
        activeDebtUpdater.accept(new DebtStateModel(
                debt.debtId(),
                debt.debtorPlayerId(),
                debt.creditorType(),
                debt.creditorPlayerId(),
                debt.amountRemaining(),
                debt.reason(),
                bankruptcyRisk,
                currentCash,
                liquidationValue,
                allowedActions(bankruptcyRisk)
        ));
    }

    private List<DebtAction> allowedActions(boolean bankruptcyRisk) {
        List<DebtAction> actions = new ArrayList<>(List.of(
                DebtAction.PAY_DEBT_NOW,
                DebtAction.MORTGAGE_PROPERTY,
                DebtAction.SELL_BUILDING,
                DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET
        ));
        if (bankruptcyRisk) {
            actions.add(DebtAction.DECLARE_BANKRUPTCY);
        }
        return actions;
    }

    private CommandResult accepted(String eventType) {
        SessionState state = sessionStateSupplier.get();
        if (state.activeDebt() == null && state.turn().phase() == TurnPhase.RESOLVING_DEBT) {
            state = new SessionState(
                    state.sessionId(),
                    state.version(),
                    state.status(),
                    state.seats(),
                    state.players(),
                    state.properties(),
                    new TurnState(state.turn().activePlayerId(), TurnPhase.WAITING_FOR_END_TURN, false, true),
                    state.pendingDecision(),
                    state.auctionState(),
                    null,
                    state.tradeState(),
                    state.winnerPlayerId()
            );
        }
        return new CommandResult(true, state, List.of(new DomainEvent(eventType, state.turn().activePlayerId(), eventType)), List.of(), List.of());
    }

    private CommandResult rejected(String code, String message) {
        return new CommandResult(false, sessionStateSupplier.get(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }

    private CommandResult unsupported() {
        return rejected("UNSUPPORTED_COMMAND", "Command is not supported by debt remediation");
    }
}
