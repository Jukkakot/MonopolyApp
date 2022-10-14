package org.example;

import org.example.components.Dices;
import org.example.spots.Spot;

public class Game {
    Board board;
    Dices dices;
    MonopolyApp p;
    float i = 0;

    public Game(MonopolyApp p) {
        this.p = p;
        board = new Board(p);
        dices = new Dices(p);

        p.p5.addButton("rollDice")
                .setValue(0)
                .setPosition((int) (Spot.spotW * 5.4), (int) (Spot.spotW * 3))
                .setSize(100,50);
    }
    public void rollDice() {
        dices.roll();
    }
    public void draw() {
        board.draw();
        dices.draw();
        i += 0.5;
    }
    public void rollDice(int theValue) {
        System.out.println("click click");
        dices.roll();
    }
}
