package fi.monopoly.components;

import fi.monopoly.components.board.Board;
import fi.monopoly.types.TurnResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import fi.monopoly.components.dices.Dices;

@AllArgsConstructor
@Getter
public class GameState {
    Players players;
    Dices dices;
    Board board;
    TurnResult prevTurnResult;
}
