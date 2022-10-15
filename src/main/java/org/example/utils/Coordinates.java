package org.example.utils;

public record Coordinates(float x, float y) {
    public double getDistance(Coordinates c) {
        return Math.sqrt(Math.pow(x - c.x, 2) + Math.pow(y - c.y, 2));
    }
}
