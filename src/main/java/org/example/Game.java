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
    Animations animations;
    float i = 0;

    public Game(MonopolyApp p) {
        this.p = p;
        board = new Board(p);
        dices = new Dices(p);
        players = new Players();
        animations = new Animations();

        Spot spot = board.getSpots().get(0);
        players.addPlayer(new Player(1, "Toka", new Token(p, spot, Color.MEDIUMPURPLE), 1), spot);
        players.addPlayer(new Player(2, "Kolmas", new Token(p, spot, Color.PINK), 2), spot);
        players.addPlayer(new Player(3, "Neljäs", new Token(p, spot, Color.DARKOLIVEGREEN), 3), spot);
        players.addPlayer(new Player(4, "Toka", new Token(p, spot, Color.TURQUOISE), 4), spot);
        players.addPlayer(new Player(5, "Kolmas", new Token(p, spot, Color.BLANCHEDALMOND), 5), spot);
        players.addPlayer(new Player(6, "Neljäs", new Token(p, spot, Color.DIMGREY), 6), spot);

        p.p5.addButton("rollDice")
                .setValue(0)
                .setPosition((int) (Spot.spotW * 5.4), (int) (Spot.spotW * 3))
                .addListener(e -> rollDice())
                .setLabel("Roll dice")
                .setSize(100, 50);
    }

    public void draw() {
        animations.updateAnimations();
        board.draw(0);
        dices.draw(0);
        players.draw();
        i += 0.5;
    }

    public void rollDice() {
        int value = dices.roll();
        Player turn = players.getTurn();
        Spot oldSpot = turn.getToken().getSpot();
        Spot newSpot = board.getNewSpot(oldSpot, value);
        animations.addAnimation(new Animation(turn.getToken(), board.getPath(oldSpot, value, turn)));
        turn.moveToken(newSpot);
        players.switchTurn();
    }
}
