package fi.monopoly.components;

import fi.monopoly.utils.Coordinates;

public interface Drawable {
    void draw(Coordinates c);

    void setHovered(boolean isHovered);

    boolean isHovered();

    Coordinates getCoords();

    void setCoords(Coordinates coords);
    Coordinates move(Coordinates coords);
}
