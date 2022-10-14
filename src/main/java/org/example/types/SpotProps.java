package org.example.types;

public record SpotProps(int x, int y, float width, float height, float rotation) {
    public SpotProps (int x, int y, float width, float height) {
        this(x, y, width, height, 0);
    }
}
