package org.example.images;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.example.components.Drawable;
import org.example.utils.Coordinates;

@ToString(callSuper = true)
@RequiredArgsConstructor
public class Deed implements Drawable {
    private boolean isDeedHovered = false;
    private final SpotImage spotImage;

    @Override
    public void draw(Coordinates c) {
        setCoords(c);
        spotImage.draw(c);
    }

    @Override
    public void setHovered(boolean isHovered) {
       this.isDeedHovered = isHovered;
    }

    @Override
    public boolean isHovered() {
        return isDeedHovered;
    }

    @Override
    public Coordinates getCoords() {
        return spotImage.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        spotImage.setCoords(coords);
    }
}
