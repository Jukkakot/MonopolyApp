package fi.monopoly.images;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Drawable;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.SpotProps;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import processing.core.PImage;

import static fi.monopoly.utils.MonopolyUtils.toColor;

@ToString(onlyExplicitlyIncluded = true)
public class Image implements Drawable {
    protected final MonopolyRuntime runtime;
    @Setter
    @Getter
    protected Coordinates coords;
    private final float width;
    private final float height;
    @Setter
    protected String imgName;
    @Getter
    @Setter
    protected boolean isHovered = false;
    protected static final float HOVER_SCALE = 1.1f;

    public Image(MonopolyRuntime runtime, SpotProps sp, String imgName) {
        this(runtime, Coordinates.of(sp), imgName, sp.w(), sp.h());
    }

    public Image(MonopolyRuntime runtime, Coordinates coords, String imgName, float width, float height) {
        this.runtime = runtime;
        this.coords = coords;
        this.imgName = imgName;
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return getScaledLength(width);
    }

    public float getHeight() {
        return getScaledLength(height);
    }

    public float getUnScaledWidth() {
        return width;
    }

    public float getUnScaledHeight() {
        return height;
    }

    public void draw(Coordinates c, Color color) {
        float rotation = c != null ? c.r() : 0;
        if (c == null) {
            c = coords;
        }
        setCoords(c);
        MonopolyApp p = runtime.app();
        p.push();

        p.translate(c.x(), c.y());
        p.rotate(MonopolyApp.radians(c.r() + rotation));
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.getImage(imgName);
        if (img != null) {
            if (color != null) {
                p.tint(MonopolyUtils.toColor(p, color));
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

    @Override
    public Coordinates move(Coordinates coords) {
        return this.coords.move(coords);
    }
}
