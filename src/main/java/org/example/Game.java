package org.example;

import javafx.scene.paint.Color;
import org.example.components.Board;
import org.example.components.Dices;
import org.example.components.Spot;
import org.example.components.Token;

public class Game {
    Board board;
    Dices dices;
    Players players;
    MonopolyApp p;
    float i = 0;

    public Game(MonopolyApp p) {
        this.p = p;
        board = new Board(p);
        dices = new Dices(p);
        players = new Players();

        players.addPlayer(new Player(1, "Toka", new Token(p, Color.MEDIUMPURPLE), 1), board.getSpots().get(0));
        players.addPlayer(new Player(2, "Kolmas", new Token(p, Color.PINK), 2), board.getSpots().get(0));
        players.addPlayer(new Player(3, "Neljäs", new Token(p, Color.AZURE), 3), board.getSpots().get(0));

        p.p5.addButton("rollDice")
                .setValue(0)
                .setPosition((int) (Spot.spotW * 5.4), (int) (Spot.spotW * 3))
                .addListener(e -> rollDice())
                .setLabel("Roll dice")
                .setSize(100, 50);
    }

    public void draw() {
        board.draw();
        dices.draw();
        players.draw();
        i += 0.5;
    }

    public void rollDice() {
        int value = dices.roll();
        Player turn = players.getTurn();
        turn.moveToken(board.getNewSpot(turn, value));
        players.switchTurn();
    }
}
