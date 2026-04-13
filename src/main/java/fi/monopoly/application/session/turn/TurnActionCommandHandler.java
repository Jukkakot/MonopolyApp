package fi.monopoly.application.session.turn;

import fi.monopoly.application.command.BuyBuildingRoundCommand;
import fi.monopoly.application.command.EndTurnCommand;
import fi.monopoly.application.command.RollDiceCommand;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.command.ToggleMortgageCommand;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.result.DomainEvent;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.session.SessionState;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TurnActionCommandHandler {
    private final String sessionId;
    private final Supplier<SessionState> currentStateSupplier;
    private final TurnActionGateway gateway;

    public TurnActionCommandHandler(
            String sessionId,
            Supplier<SessionState> currentStateSupplier,
            TurnActionGateway gateway
    ) {
        this.sessionId = sessionId;
        this.currentStateSupplier = currentStateSupplier;
        this.gateway = gateway;
    }

    public boolean supports(SessionCommand command) {
        return command instanceof RollDiceCommand
                || command instanceof EndTurnCommand
                || command instanceof BuyBuildingRoundCommand
                || command instanceof ToggleMortgageCommand;
    }

    public CommandResult handle(SessionCommand command) {
        if (command instanceof RollDiceCommand rollDiceCommand) {
            return handleRollDice(rollDiceCommand);
        }
        if (command instanceof EndTurnCommand endTurnCommand) {
            return handleEndTurn(endTurnCommand);
        }
        if (command instanceof BuyBuildingRoundCommand buyBuildingRoundCommand) {
            return handleBuyBuildingRound(buyBuildingRoundCommand);
        }
        if (command instanceof ToggleMortgageCommand toggleMortgageCommand) {
            return handleToggleMortgage(toggleMortgageCommand);
        }
        return rejected("UNSUPPORTED_TURN_ACTION", "Turn action command is not supported");
    }

    private CommandResult handleRollDice(RollDiceCommand command) {
        if (!isCurrentActor(command.sessionId(), command.actorPlayerId())) {
            return rejected("WRONG_TURN_ACTOR", "Only the active player can roll dice");
        }
        if (isTurnActionBlocked(currentStateSupplier.get().turn().phase())) {
            return rejected("ROLL_NOT_ALLOWED", "Dice can only be rolled during the rolling phase");
        }
        return gateway.rollDice()
                ? accepted("DiceRolled", command.actorPlayerId())
                : rejected("ROLL_FAILED", "Dice roll could not be started");
    }

    private CommandResult handleEndTurn(EndTurnCommand command) {
        if (!isCurrentActor(command.sessionId(), command.actorPlayerId())) {
            return rejected("WRONG_TURN_ACTOR", "Only the active player can end the turn");
        }
        if (isTurnActionBlocked(currentStateSupplier.get().turn().phase())) {
            return rejected("END_TURN_NOT_ALLOWED", "Turn can only end during the end-turn phase");
        }
        return gateway.endTurn()
                ? accepted("TurnEnded", command.actorPlayerId())
                : rejected("END_TURN_FAILED", "Turn could not be ended");
    }

    private CommandResult handleBuyBuildingRound(BuyBuildingRoundCommand command) {
        if (!isCurrentActor(command.sessionId(), command.actorPlayerId())) {
            return rejected("WRONG_TURN_ACTOR", "Only the active player can buy buildings");
        }
        if (!gateway.buyBuildingRound(command.propertyId())) {
            return rejected("BUILD_ROUND_FAILED", "Building round purchase failed");
        }
        return accepted("BuildingRoundBought", command.propertyId());
    }

    private CommandResult handleToggleMortgage(ToggleMortgageCommand command) {
        if (!isCurrentActor(command.sessionId(), command.actorPlayerId())) {
            return rejected("WRONG_TURN_ACTOR", "Only the active player can change mortgages");
        }
        if (!gateway.toggleMortgage(command.propertyId())) {
            return rejected("MORTGAGE_TOGGLE_FAILED", "Mortgage action failed");
        }
        return accepted("MortgageToggled", command.propertyId());
    }

    private boolean isCurrentActor(String commandSessionId, String actorPlayerId) {
        SessionState state = currentStateSupplier.get();
        return Objects.equals(sessionId, commandSessionId)
                && Objects.equals(state.turn().activePlayerId(), actorPlayerId);
    }

    private boolean isTurnActionBlocked(TurnPhase phase) {
        return phase == TurnPhase.WAITING_FOR_DECISION
                || phase == TurnPhase.WAITING_FOR_AUCTION
                || phase == TurnPhase.RESOLVING_DEBT
                || phase == TurnPhase.GAME_OVER;
    }

    private CommandResult accepted(String eventType, String detail) {
        return new CommandResult(true, currentStateSupplier.get(), List.of(new DomainEvent(eventType, detail, null)), List.of(), List.of());
    }

    private CommandResult rejected(String code, String message) {
        return new CommandResult(false, currentStateSupplier.get(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }
}
