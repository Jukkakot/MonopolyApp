package org.example.images;

import javafx.scene.paint.Color;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.Drawable;
import org.example.components.Spot;
import org.example.components.Token;
import org.example.types.SpotProps;
import org.example.utils.Coordinates;
import processing.core.PApplet;
import processing.core.PImage;

import static org.example.utils.Utils.toColor;

public class Image implements Drawable {

    protected float x;
    protected float y;
    protected final float rotation;
    protected float width;
    protected float height;
    @Setter
    protected String imgName;
    protected PApplet p;

    public Image(PApplet p, int x, int y, float width, float height, float rotation) {
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.width = width;
        this.height = height;
        this.p = p;
    }

    public Image(PApplet p, SpotProps sp) {
        this(p, sp.x(), sp.y(), sp.width(), sp.height(), sp.rotation());
    }

    public Image(PApplet p, SpotProps sp, String imgName) {
        this(p, sp);
        this.imgName = imgName;
    }

    public Image(PApplet p, Spot spot) {
        this.p = p;
        this.x = spot.getX();
        this.y = spot.getY();
        this.rotation = spot.getRotation();

    }

    public Image(PApplet p, Spot spot, String imgName) {
        this(p, spot);
        this.imgName = imgName;

        this.width = Token.TOKEN_RADIUS;
        this.height= Token.TOKEN_RADIUS;
    }

    public Coordinates getCoords() {
        return new Coordinates(x, y);
    }

    public void setCoords(Coordinates coords) {
        this.x = coords.x();
        this.y = coords.y();
    }

    public static void defaultDraw(PApplet p, Spot spot, int radius, Color color) {
        defaultDraw(p, new Coordinates(spot.getX(), spot.getY()), radius, color);
    }

    public static void defaultDraw(PApplet p, Coordinates coords, int radius, Color color) {
        p.push();

        p.fill(toColor(p, color));
        p.stroke(0);
        p.strokeWeight(5);
        p.circle(coords.x(), coords.y(), radius);

        p.pop();
    }

    @Override
    public void draw(float rotate) {
        draw(getCoords());
    }

    @Override
    public void draw() {
        draw(0);
    }

    public void draw(Coordinates coords, Color color) {
        p.push();

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians((rotation)));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.tint(toColor(p, color));
        p.image(img, 0, 0);

        p.pop();
    }

    @Override
    public void draw(Coordinates coordinates) {
        p.push();

        p.translate(x, y);
        p.rotate(MonopolyApp.radians((rotation)));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.pop();
    }

}
