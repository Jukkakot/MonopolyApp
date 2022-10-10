package org.example;

import processing.core.PApplet;

public class MonopolyApp extends PApplet {
    Game game;
    public void settings() {
        size(1700,1000);
    }

    public void setup() {
        game = new Game(this);
    }

    public void draw() {
        game.draw();
    }

}
