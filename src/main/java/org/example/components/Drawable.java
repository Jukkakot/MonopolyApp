package org.example.components;

import org.example.utils.Coordinates;

public interface Drawable {
    void draw(Coordinates c);

    void setHovered(boolean isHovered);

    boolean isHovered();

    Coordinates getCoords();

    void setCoords(Coordinates coords);
}
