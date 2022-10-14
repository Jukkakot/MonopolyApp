package org.example;

import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotType;
import processing.core.PImage;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Board extends MonopolyApp implements Drawable {
    List<Spot> spots = new ArrayList<>();
    final List<SpotType> spotTypes = Arrays.asList(SpotType.CORNER1, SpotType.B1, SpotType.COMMUNITY1, SpotType.B2,
            SpotType.TAX1, SpotType.RR1, SpotType.LB1, SpotType.CHANCE1, SpotType.LB2, SpotType.LB3, SpotType.CORNER2,
            SpotType.P1, SpotType.U1, SpotType.P2, SpotType.P3, SpotType.RR2, SpotType.O1, SpotType.COMMUNITY2, SpotType.O2, SpotType.O3, SpotType.CORNER3,
            SpotType.R1, SpotType.CHANCE2, SpotType.R2, SpotType.R3, SpotType.RR3, SpotType.Y1, SpotType.Y2, SpotType.U2, SpotType.Y3, SpotType.CORNER4,
            SpotType.G1, SpotType.G2, SpotType.COMMUNITY3, SpotType.G3, SpotType.RR4, SpotType.CHANCE3, SpotType.DB1, SpotType.TAX2, SpotType.DB2);
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
            SpotType sT = spotTypes.get(spots.indexOf(s));
            s.setImage(sT);
        });
    }

    @Override
    public void draw(float rotate) {
        drawBackground(rotate);
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
}
