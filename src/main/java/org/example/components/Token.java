package org.example.components;

import lombok.Getter;
import lombok.Setter;
import org.example.utils.Coordinates;
import org.example.images.Image;
import org.example.Drawable;
import processing.core.PApplet;
import javafx.scene.paint.Color;

public class Token implements Drawable {
    private final Image img;
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
        this.img = new Image(p, spot, "Token.png");
        img.setCoords(spot.getCoordinates());
    }
    @Override
    public void draw(float rotate) {
        img.draw(img.getCoords(),color);
    }
    @Override
    public void draw(Coordinates coords) {
        img.draw(coords, color);
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
        this.draw(0);
    }
}
