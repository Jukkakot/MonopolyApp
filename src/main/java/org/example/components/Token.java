package org.example.components;

import lombok.Getter;
import lombok.Setter;
import org.example.utils.Coordinates;
import org.example.images.Image;
import org.example.Drawable;
import processing.core.PApplet;
import javafx.scene.paint.Color;

public class Token implements Drawable {
    //    private final Image img;
    private final Color color;
    @Setter
    @Getter
    private Spot spot;
    private PApplet p;
    public static final int TOKEN_RADIUS = 25;

    public Token(PApplet p, Color color) {
//        this.img = new Image(p, spot, imgName);
        this.p = p;
        this.color = color;
    }
    @Override
    public void draw(float rotate) {
        spot.drawPlayers();
    }

    public void draw(Coordinates coords) {
        Image.defaultDraw(p, coords, TOKEN_RADIUS, color);
    }

    @Override
    public void draw() {
        draw(0);
    }
}
