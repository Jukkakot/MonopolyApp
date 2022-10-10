package org.example.spots;

import org.example.MonopolyApp;
import org.example.Player;
import org.example.images.Image;
import org.example.types.SpotType;
import processing.core.PApplet;


import java.util.List;

public class Spot extends MonopolyApp implements Drawable {
    SpotType spotType;
    List<Player> playerList;
    Image image;
    protected PApplet p;
    public Spot(Image image) {
        this.image = image;
    }

    @Override
    public void draw(float rotate) {
        image.draw(rotate);
    }

    @Override
    public void draw() {
        image.draw();
    }
}
