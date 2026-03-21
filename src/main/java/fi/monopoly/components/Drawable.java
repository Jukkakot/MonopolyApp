package fi.monopoly.components;

import fi.monopoly.utils.Coordinates;

public interface Drawable {
    void draw(Coordinates c);

    boolean isHovered();

    void setHovered(boolean isHovered);

    Coordinates getCoords();

    void setCoords(Coordinates coords);

    Coordinates move(Coordinates coords);
}
