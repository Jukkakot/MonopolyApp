package org.example.images;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.MonopolyApp;
import org.example.components.Clickable;
import org.example.components.Drawable;
import org.example.components.event.MonopolyEventListener;
import org.example.utils.Coordinates;
import org.example.utils.SpotProps;
import processing.core.PImage;
import processing.event.Event;
import processing.event.MouseEvent;

import static org.example.utils.Utils.toColor;

@ToString(includeFieldNames = false)
public class Image implements Drawable, Clickable, MonopolyEventListener {
    @Setter
    @Getter
    protected Coordinates coords;
    protected float width;
    protected float height;
    @Setter
    protected String imgName;
    @Getter
    @Setter
    protected boolean isHovered = false;
    protected static final MonopolyApp p = MonopolyApp.self;
    protected static final float HOVER_SCALE = 1.1f;

    public Image(SpotProps sp, String imgName) {
        this(Coordinates.of(sp), imgName, sp.w(), sp.h());
    }

    public Image(Coordinates coords) {
        this(coords, null, 0, 0);
    }

    public Image(Coordinates coords, String imgName, float width, float height) {
        MonopolyApp.addListener(this);
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

    @Override
    public void onClick() {
        System.out.println("Clicked " + this);
    }

    @Override
    public boolean onEvent(Event event) {
        boolean eventConsumed = false;
        if (isHovered() && event instanceof MouseEvent mouseEvent) {
            if (MouseEvent.CLICK == mouseEvent.getAction()) {
                onClick();
                eventConsumed = true;
            }
        }
        return eventConsumed;
    }
}
