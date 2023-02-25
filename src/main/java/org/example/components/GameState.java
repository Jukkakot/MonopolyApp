package org.example.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.components.board.Board;
import org.example.components.board.Path;
import org.example.components.dices.Dices;
import org.example.types.TurnResult;

@AllArgsConstructor
@Getter
public class GameState {
    Players players;
    Dices dices;
    Board board;
    Path path;
    TurnResult prevTurnResult;
}
