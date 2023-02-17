package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import org.example.Drawable;
import org.example.components.spots.Spot;
import org.example.images.Image;
import org.example.utils.Coordinates;

public class Token implements Drawable {
    @Getter
    private final Image img;
    @Getter
    private final Color color;
    @Setter
    @Getter
    private Spot spot;
    public static final int TOKEN_RADIUS = 25;

    public Token(Spot spot, Color color) {
//        this.img = new Image( spot, imgName);
        this.color = color;
        this.spot = spot;
        this.img = new Image(spot.getTokenCoords(), "Token.png");
    }

    @Override
    public void draw(Coordinates coords) {
        img.draw(color, coords);
//        Image.defaultDraw( coords, TOKEN_RADIUS, color);
    }

    @Override
    public Coordinates getCoords() {
        return img.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        img.setCoords(coords);
    }

    @Override
    public void draw() {
        this.draw(null);
    }
}
