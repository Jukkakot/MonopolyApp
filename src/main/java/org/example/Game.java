package org.example;

import processing.core.PApplet;

public class Game {
    Board board;

    public Game(PApplet p) {
        board = new Board(p);
    }
    public void draw() {
        board.draw();
    }
}
