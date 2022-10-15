package org.example.components;

import lombok.Getter;
import org.example.Drawable;
import org.example.MonopolyApp;
import org.example.Player;
import org.example.types.SpotType;
import processing.core.PImage;


import java.util.ArrayList;
import java.util.List;

public class Board extends MonopolyApp implements Drawable {
    @Getter
    private final List<Spot> spots = new ArrayList<>();
    MonopolyApp p;

    public Board(MonopolyApp p) {
        this.p = p;
        initSpots();
        initImages();
    }

    private void initSpots() {
        int currX = (Spot.spotH + 9 * Spot.spotW) + Spot.spotH / 2;
        int currY = (Spot.spotH + 9 * Spot.spotW) + Spot.spotH / 2;
        int currRotation = 0;

        //BOTTOM ROW
        spots.add(new Spot(p, currX, currY, currRotation));
        currX -= Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(new Spot(p, currX, currY, currRotation));
            currX -= Spot.spotW;

        }
        currX += Spot.spotW;
        currX -= Spot.spotH / 2 + Spot.spotW / 2;
        spots.add(new Spot(p, currX, currY, currRotation));

        //LEFT COLUMN
        currRotation += 90;
        currY -= Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(new Spot(p, currX, currY, currRotation));
            currY -= Spot.spotW;
        }
        currY += Spot.spotW;
        currY -= Spot.spotH / 2 + Spot.spotW / 2;

        //TOP ROW
        currRotation += 90;
        spots.add(new Spot(p, currX, currY, currRotation));
        currX += Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(new Spot(p, currX, currY, currRotation));
            currX += Spot.spotW;
        }
        currX -= Spot.spotW;
        currX += Spot.spotH / 2 + Spot.spotW / 2;
        spots.add(new Spot(p, currX, currY, currRotation));

        //RIGHT COLUMN
        currRotation += 90;
        currY += Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(new Spot(p, currX, currY, currRotation));
            currY += Spot.spotW;
        }
    }

    private void initImages() {
        spots.forEach(s -> {
            SpotType sT = SpotType.spotTypes.get(spots.indexOf(s));
            s.setImage(sT);
        });
    }

    @Override
    public void draw(float rotate) {
        drawBackground(rotate);
//        spots.forEach(Spot::drawPlayers);
    }

    @Override
    public void draw() {
        draw(0);
    }

    private void drawBackground(float rotate) {
        p.push();
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get("Background.png");
        img.resize(Spot.spotW * 9, Spot.spotW * 9);
        p.image(MonopolyApp.IMAGES.get("Background.png"), (float) (Spot.spotW * 6), (float) (Spot.spotW * 6));
        spots.forEach(s -> s.draw(rotate));
        p.pop();
    }

    public Spot getNewSpot(Player player, int diceValue) {
        int currSpot = spots.indexOf(player.getToken().getSpot());
        return spots.get((currSpot + diceValue) % spots.size());
    }
}
