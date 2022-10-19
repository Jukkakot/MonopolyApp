package org.example.utils;

import java.awt.*;

public record Coordinates(float x, float y, float rotation) {
    public double getDistance(Coordinates c) {
        return Math.sqrt(Math.pow(x - c.x, 2) + Math.pow(y - c.y, 2));
    }
    public Coordinates(float x, float y) {
        this(x, y, 0);
    }
    public Coordinates move(int x, int y) {
        return new Coordinates(this.x + x, this.y + y);
    }
}
