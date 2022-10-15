package org.example.components;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.Player;
import org.example.types.SpotType;
import org.example.utils.Coordinates;
import processing.core.PImage;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Board extends MonopolyApp {
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

    public void draw(float rotate) {
        drawBackground(rotate);
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

    public Spot getNewSpot(Spot spot, int diceValue) {
        int currSpot = spots.indexOf(spot);
        return spots.get((currSpot + diceValue) % spots.size());
    }

    public List<Coordinates> getPath(Spot start, int value, Player player) {
        List<Spot> result = new ArrayList<>();
        Spot nextSpot = getNewSpot(start, 1);
        result.add(nextSpot);
        for (int i = 0; i < value; i++) {
            result.add(nextSpot);
            nextSpot = getNewSpot(nextSpot, 1);
        }
        return result.stream().map(s -> s.getCoordinates(player)).collect(Collectors.toList());
    }
}
