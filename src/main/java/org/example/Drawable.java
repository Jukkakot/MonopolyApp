package org.example;

import org.example.utils.Coordinates;

public interface Drawable {
    void draw();
    void draw(Coordinates c);
    Coordinates getCoords();
    void setCoords(Coordinates coords);
}
