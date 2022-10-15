package org.example;

import org.example.utils.Coordinates;

public interface Drawable {
    void draw(float rotate);
    void draw();
    void draw(Coordinates coordinates);
    Coordinates getCoords();
    void setCoords(Coordinates coords);
}
