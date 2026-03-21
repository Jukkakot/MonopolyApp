package fi.monopoly.components.turn;

import fi.monopoly.components.board.Path;
import fi.monopoly.types.DiceState;

public record MovePlayerEffect(Path path, DiceState diceState) implements TurnEffect {
}
