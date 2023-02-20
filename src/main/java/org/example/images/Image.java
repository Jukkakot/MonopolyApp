package org.example.images;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.components.Drawable;
import org.example.components.PlayerToken;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import processing.core.PImage;

import static org.example.utils.Utils.toColor;

public abstract class Image implements Drawable {
    @Setter
    @Getter
    protected Coordinates coords;
    protected float width;
    protected float height;
    @Setter
    protected String imgName;
    @Getter
    protected boolean isHovered = false;
    protected static final MonopolyApp p = MonopolyApp.self;
    protected static final float HOVER_SCALE = 1.1f;

    public Image(SpotProps sp) {
        this(sp, null);
    }

    public Image(SpotProps sp, String imgName) {
        this.coords = new Coordinates(sp.x(), sp.y(), sp.r());
        this.width = sp.w();
        this.height = sp.h();
        this.imgName = imgName;
    }

    public Image(Coordinates coords) {
        this.coords = coords;
    }

    public Image(Coordinates coords, String imgName) {
        this(coords);
        this.imgName = imgName;

        this.width = PlayerToken.TOKEN_RADIUS;
        this.height = PlayerToken.TOKEN_RADIUS;
    }

    public float getWidth() {
        return getScaledLength(width);
    }

    public float getHeight() {
        return getScaledLength(height);
    }

    public void draw(Coordinates c, Color color) {
        float rotation = c != null ? c.r() : 0;
        if (c == null) {
            c = coords;
        }
        p.push();

        p.translate(c.x(), c.y());
        p.rotate(MonopolyApp.radians(c.r() + rotation));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(imgName);
        if (img != null) {
            if (color != null) {
                p.tint(toColor(color));
            }
            p.image(img, 0, 0, getWidth(), getHeight());
        }

        p.pop();
    }

    @Override
    public void draw(Coordinates c) {
        draw(c, null);
    }

    protected float getScaledLength(float length) {
        return isHovered ? length * HOVER_SCALE : length;
    }
}
