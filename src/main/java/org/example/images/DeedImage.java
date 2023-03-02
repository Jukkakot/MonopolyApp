package org.example.images;

import lombok.Getter;
import org.example.components.Drawable;
import org.example.utils.Coordinates;
import org.example.utils.Utils;

public class DeedImage implements Drawable {
    private final SpotImage spotImage;
    @Getter
    private boolean isHovered = false;

    public DeedImage(SpotImage spotImage) {
        this.spotImage = spotImage;
    }

    @Override
    public void draw(Coordinates c) {
        spotImage.draw(c);
        spotImage.setHovered(false); // deeds and spots have their own hover effect.. not the greatest solution
        isHovered = Utils.isMouseInArea(c, spotImage.getWidth(), spotImage.getHeight());
    }

    @Override
    public Coordinates getCoords() {
        return spotImage.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        spotImage.setCoords(coords);
    }

    public void click() {
        System.out.println("Clicked " + spotImage.getSpotType());
    }
}
