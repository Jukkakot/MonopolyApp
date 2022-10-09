package org.example;

import processing.core.PApplet;

public class MonopolyApp extends PApplet {
    @Override
    public void settings() {
        size(500, 500);
    }

    @Override
    public void draw() {
        background(0);
        fill(255, 0, 0);
        ellipse(mouseX, mouseY, 100, 100);
    }

    public static void main(String[] args) {
        PApplet.main(MonopolyApp.class.getCanonicalName());
    }
}