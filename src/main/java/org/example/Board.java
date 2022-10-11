package org.example;

import org.example.spots.Drawable;
import org.example.spots.Spot;
import org.example.types.SpotType;
import processing.core.PApplet;


import java.util.ArrayList;
import java.util.List;

public class Board implements Drawable {
    List<Spot> spots = new ArrayList<>();
    PApplet p;

    public Board(PApplet p) {
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
        spots.add(new Spot(p, currX, currY, currRotation));

        //TOP ROW
        currRotation += 90;
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
        int i = 0;
        spots.get(i).setImage(SpotType.CORNER1);
        spots.get(++i).setImage(SpotType.B1);
        spots.get(++i).setImage(SpotType.COMMUNITY1);
        spots.get(++i).setImage(SpotType.B2);
        spots.get(++i).setImage(SpotType.TAX1);
        spots.get(++i).setImage(SpotType.RR1);
        spots.get(++i).setImage(SpotType.LB1);
        spots.get(++i).setImage(SpotType.CHANCE1);
        spots.get(++i).setImage(SpotType.LB2);
        spots.get(++i).setImage(SpotType.LB3);

        spots.get(++i).setImage(SpotType.CORNER2);
        spots.get(++i).setImage(SpotType.P1);
        spots.get(++i).setImage(SpotType.U1);
        spots.get(++i).setImage(SpotType.P2);
        spots.get(++i).setImage(SpotType.P3);
        spots.get(++i).setImage(SpotType.RR2);
        spots.get(++i).setImage(SpotType.O1);
        spots.get(++i).setImage(SpotType.COMMUNITY2);
        spots.get(++i).setImage(SpotType.O2);
        spots.get(++i).setImage(SpotType.O3);

        spots.get(++i).setImage(SpotType.CORNER2);
        spots.get(++i).setImage(SpotType.R1);
        spots.get(++i).setImage(SpotType.CHANCE2);
        spots.get(++i).setImage(SpotType.R2);
        spots.get(++i).setImage(SpotType.R3);
        spots.get(++i).setImage(SpotType.RR3);
        spots.get(++i).setImage(SpotType.Y1);
        spots.get(++i).setImage(SpotType.Y2);
        spots.get(++i).setImage(SpotType.U2);
        spots.get(++i).setImage(SpotType.Y3);

        spots.get(++i).setImage(SpotType.CORNER4);
        spots.get(++i).setImage(SpotType.G1);
        spots.get(++i).setImage(SpotType.G2);
        spots.get(++i).setImage(SpotType.COMMUNITY3);
        spots.get(++i).setImage(SpotType.G3);
        spots.get(++i).setImage(SpotType.RR4);
        spots.get(++i).setImage(SpotType.CHANCE3);
        spots.get(++i).setImage(SpotType.DB1);
        spots.get(++i).setImage(SpotType.TAX2);
        spots.get(++i).setImage(SpotType.DB2);

    }

    @Override
    public void draw(float rotate) {
        spots.forEach(s -> s.draw(rotate));
    }

    @Override
    public void draw() {
        draw(0);
    }
}
