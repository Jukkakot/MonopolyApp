package org.example.images;

import javafx.scene.paint.Color;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.Drawable;
import org.example.components.Token;
import org.example.utils.SpotProps;
import org.example.utils.Coordinates;
import processing.core.PApplet;
import processing.core.PImage;

import static org.example.utils.Utils.toColor;

public class Image implements Drawable {

    protected Coordinates coords;
    protected float width;
    protected float height;
    @Setter
    protected String imgName;
    protected PApplet p;

    public Image(PApplet p, SpotProps sp) {
        this.p = p;
        this.coords = new Coordinates(sp.x(), sp.y(), sp.rotation());
        this.width = sp.width();
        this.height = sp.height();
    }

    public Image(PApplet p, SpotProps sp, String imgName) {
        this(p, sp);
        this.imgName = imgName;
    }

    public Image(PApplet p, Coordinates coords) {
        this.p = p;
        this.coords = coords;
    }

    public Image(PApplet p, Coordinates coords, String imgName) {
        this(p, coords);
        this.imgName = imgName;

        this.width = Token.TOKEN_RADIUS;
        this.height = Token.TOKEN_RADIUS;
    }

    public Coordinates getCoords() {
        return coords;
    }

    public void setCoords(Coordinates coords) {
        this.coords = coords;
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
    public void draw() {
        draw(coords);
    }

    public void draw(Color color) {
        p.push();

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians((coords.rotation())));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.tint(toColor(p, color));
        p.image(img, 0, 0);

        p.pop();
    }

    @Override
    public void draw(Coordinates c) {
        float rotation = c != null ? c.rotation() : 0;
        p.push();

        p.translate(coords.x(), coords.y());
        p.rotate(MonopolyApp.radians(coords.rotation() + rotation));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.pop();
    }

}
