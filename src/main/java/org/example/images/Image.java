package org.example.images;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.components.Drawable;
import org.example.components.Token;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import processing.core.PImage;

import static org.example.utils.Utils.toColor;

public class Image implements Drawable {

    protected Coordinates coords;
    protected float width;
    protected float height;
    @Setter
    @Getter
    protected String imgName;
    @Getter
    protected boolean isHovered = false;
    protected static final MonopolyApp p = MonopolyApp.self;
    protected static final float HOVER_SCALE = 1.1f;

    public Image(SpotProps sp) {
        this.coords = new Coordinates(sp.x(), sp.y(), sp.r());
        this.width = sp.w();
        this.height = sp.h();
    }

    public Image(SpotProps sp, String imgName) {
        this(sp);
        this.imgName = imgName;
    }

    public Image(Coordinates coords) {
        this.coords = coords;
    }

    public Image(Coordinates coords, String imgName) {
        this(coords);
        this.imgName = imgName;

        this.width = Token.TOKEN_RADIUS;
        this.height = Token.TOKEN_RADIUS;
    }

    public float getWidth() {
        return getScaledLength(width);
    }

    public float getHeight() {
        return getScaledLength(height);
    }

    public Coordinates getCoords() {
        return coords;
    }

    public void setCoords(Coordinates coords) {
        this.coords = coords;
    }

    public static void defaultDraw(Coordinates coords, int radius, Color color) {
        MonopolyApp p = MonopolyApp.self;
        p.push();

        p.fill(toColor(color));
        p.stroke(0);
        p.strokeWeight(5);
        p.circle(coords.x(), coords.y(), radius);

        p.pop();
    }

    @Override
    public void draw() {
        draw(coords);
    }

    public void draw(Color color, Coordinates coords) {
        p.push();
        p.tint(toColor(color));
        draw(coords);
        p.pop();
    }

    public void draw(Color color) {
        p.push();

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians((coords.r())));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(imgName);
        p.tint(toColor(color));
        p.image(img, 0, 0, getWidth(), getHeight());

        p.pop();
    }

    @Override
    public void draw(Coordinates c) {
        float rotation = c != null ? c.r() : 0;
        p.push();

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians(coords.r() + rotation));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(imgName);
        p.image(img, 0, 0, getWidth(), getHeight());

        p.pop();
    }

    public static void defaultDraw(SpotProps sp, String imgName, boolean pushPop) {
        MonopolyApp p = MonopolyApp.self;
        if (pushPop) {
            p.push();
        }

        p.translate(sp.x(), sp.y());
        p.rotate(MonopolyApp.radians(sp.r()));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(imgName);
        p.image(img, 0, 0, sp.w(),sp.h());

        if (pushPop) {
            p.pop();
        }
    }

    protected float getScaledLength(float length) {
        return isHovered ? length * HOVER_SCALE : length;
    }
}
