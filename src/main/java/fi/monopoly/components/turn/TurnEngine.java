package fi.monopoly.components.turn;

import fi.monopoly.components.Player;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.TurnResult;

public class TurnEngine {

    public TurnPlan createMovementPlan(Player turnPlayer, Board board, DiceValue diceValue) {
        Spot startSpot = turnPlayer.getSpot();
        Spot targetSpot = board.getNewSpot(startSpot, diceValue.value(), resolvePathMode(diceValue.diceState()));
        if (DiceState.JAIL.equals(diceValue.diceState())) {
            Path jailPath = board.getPath(turnPlayer, board.getPathWithCriteria(fi.monopoly.types.SpotType.JAIL), fi.monopoly.types.PathMode.FLY);
            return TurnPlan.of(TurnPhase.MOVING, new MovePlayerEffect(jailPath, diceValue.diceState()));
        }
        Path path = board.getPath(turnPlayer, targetSpot, resolvePathMode(diceValue.diceState()));
        return TurnPlan.of(TurnPhase.MOVING, new MovePlayerEffect(path, diceValue.diceState()));
    }

    public TurnPlan createFollowUpPlan(Player turnPlayer, Board board, TurnResult turnResult, DiceState diceState) {
        if (turnResult != null) {
            Path path = board.getPathWithCriteria(turnResult, turnPlayer);
            return TurnPlan.of(TurnPhase.MOVING, new MovePlayerEffect(path, diceState));
        }
        if (DiceState.DOUBLES.equals(diceState)) {
            return TurnPlan.of(TurnPhase.WAITING_FOR_EXTRA_ROLL, new ShowDiceEffect());
        }
        return TurnPlan.of(TurnPhase.WAITING_TO_END_TURN, new ShowEndTurnEffect());
    }

    public TurnPlan endTurn(boolean switchTurns) {
        return TurnPlan.of(TurnPhase.TURN_FINISHED, new EndTurnEffect(switchTurns));
    }

    private fi.monopoly.types.PathMode resolvePathMode(DiceState diceState) {
        if (DiceState.DEBUG_REROLL.equals(diceState) || DiceState.JAIL.equals(diceState)) {
            return fi.monopoly.types.PathMode.FLY;
        }
        return fi.monopoly.types.PathMode.NORMAL;
    }
}
