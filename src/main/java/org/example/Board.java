package org.example;

import org.example.images.PropertyImage;
import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.PropertyType;
import processing.core.PApplet;


import java.util.ArrayList;
import java.util.List;

public class Board implements Drawable {
    List<Spot> spots = new ArrayList<>();
    public Board(PApplet p) {
        spots.add(new Spot(new PropertyImage(p, 0,0, PropertyType.B1)));
        spots.add(new Spot(new PropertyImage(p,200,200, PropertyType.B1)));
        spots.add(new Spot(new PropertyImage(p,400,400, PropertyType.B1)));
    }

    @Override
    public void draw(float rotate) {
        spots.forEach(Spot::draw);
    }

    @Override
    public void draw() {
        draw(0);
    }
}
