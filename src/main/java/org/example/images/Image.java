package org.example.images;

import lombok.Setter;
import org.example.MonopolyApp;
import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotProps;
import processing.core.PApplet;
import processing.core.PImage;

public class Image extends MonopolyApp implements Drawable {
    protected final int x;
    protected final int y;
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

    public static void defaultDraw(PApplet p, int x, int y) {
        p.push();

        p.stroke(0);
        p.strokeWeight(10);
        p.point(x, y);

        p.pop();
    }

    @Override
    public void draw(float rotate) {
        p.push();

        p.translate(x, y);
        p.rotate(radians((rotation + rotate)));
        p.imageMode(CENTER);
        PImage img = IMAGES.get(imgName);
        img.resize((int) width, (int) height);
        p.image(img, 0, 0);

        p.pop();
    }
    @Override
    public void draw() {
        draw(0);
    }

}
