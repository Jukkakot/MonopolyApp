package org.example;

import processing.core.PApplet;

public class Game {
    Board board;
    PApplet p;
    float i = 0;

    public Game(PApplet p) {
        this.p = p;
        board = new Board(p);
    }
    public void draw() {
        board.draw();
        i += 0.5;
    }
}
