package org.example.components;

import lombok.Getter;
import lombok.Setter;
import org.example.components.spots.Spot;
import org.example.utils.Coordinates;
import org.example.images.Image;
import processing.core.PApplet;
import javafx.scene.paint.Color;

public class Token implements Drawable {
    @Getter
    private final Image img;
    @Getter
    private final Color color;
    @Setter
    @Getter
    private Spot spot;
    private PApplet p;
    public static final int TOKEN_RADIUS = 25;

    public Token(PApplet p, Spot spot, Color color) {
//        this.img = new Image(p, spot, imgName);
        this.p = p;
        this.color = color;
        this.spot = spot;
        this.img = new Image(p, spot.getTokenCoords(), "Token.png");
    }

    @Override
    public void draw(Coordinates coords) {
        img.draw(color);
//        Image.defaultDraw(p, coords, TOKEN_RADIUS, color);
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
