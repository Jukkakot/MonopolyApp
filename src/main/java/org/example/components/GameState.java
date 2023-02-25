package org.example.components;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.components.board.Board;
import org.example.components.board.Path;
import org.example.components.dices.Dices;

@AllArgsConstructor
@Getter
public class GameState {
    Players players;
    Dices dices;
    Board board;
    Path path;
}
