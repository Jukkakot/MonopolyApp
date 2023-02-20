package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import org.example.components.spots.Spot;
import org.example.images.Image;
import org.example.utils.Coordinates;

public class PlayerToken extends Image {
    @Getter
    protected final Color color;
    @Getter
    protected Spot spot;
    public static final int TOKEN_RADIUS = 25;

    public PlayerToken(Spot spot, Color color) {
        super(spot.getTokenCoords(), "Token.png");
        this.color = color;
        this.spot = spot;
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, color);
    }

    public void draw() {
        draw(null);
    }
}
