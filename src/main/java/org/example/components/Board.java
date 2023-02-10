package org.example.components;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.components.spots.SpotFactory;
import org.example.types.SpotTypeEnum;
import org.example.utils.Coordinates;
import processing.core.PImage;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Board {
    @Getter
    private final List<Spot> spots = new ArrayList<>();
    MonopolyApp p;

    public Board(MonopolyApp p) {
        this.p = p;
        initSpots();
    }

    private void initSpots() {
        int currX = (Spot.spotH + 9 * Spot.spotW) + Spot.spotH / 2;
        int currY = (Spot.spotH + 9 * Spot.spotW) + Spot.spotH / 2;
        int currRotation = 0;

        //BOTTOM ROW
        spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
        currX -= Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
            currX -= Spot.spotW;

        }
        currX += Spot.spotW;
        currX -= Spot.spotH / 2 + Spot.spotW / 2;
        spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));

        //LEFT COLUMN
        currRotation += 90;
        currY -= Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
            currY -= Spot.spotW;
        }
        currY += Spot.spotW;
        currY -= Spot.spotH / 2 + Spot.spotW / 2;

        //TOP ROW
        currRotation += 90;
        spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
        currX += Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
            currX += Spot.spotW;
        }
        currX -= Spot.spotW;
        currX += Spot.spotH / 2 + Spot.spotW / 2;
        spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));

        //RIGHT COLUMN
        currRotation += 90;
        currY += Spot.spotH / 2 + Spot.spotW / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(p, new Coordinates(currX, currY, currRotation), SpotTypeEnum.SPOT_TYPE_ENUMS.get(spots.size())));
            currY += Spot.spotW;
        }
    }

    public void draw(Coordinates c) {
        drawBackground(c);
        spots.forEach(s -> s.draw(c));
    }

    private void drawBackground(Coordinates c) {
        p.push();
        p.imageMode(p.CENTER);
        PImage img = MonopolyApp.IMAGES.get("Background.png");
        img.resize(Spot.spotW * 9, Spot.spotW * 9);
        p.image(MonopolyApp.IMAGES.get("Background.png"), (float) (Spot.spotW * 6), (float) (Spot.spotW * 6));
        spots.forEach(s -> s.draw(c));
        p.pop();
    }

    public Spot getNewSpot(Spot spot, int diceValue) {
        int currSpot = spots.indexOf(spot);
        return spots.get((currSpot + diceValue) % spots.size());
    }

    public List<Coordinates> getPath(Spot start, int value, Player player) {
        List<Spot> result = new ArrayList<>();
        Spot nextSpot = getNewSpot(start, 1);
        for (int i = 0; i < value; i++) {
            result.add(nextSpot);
            nextSpot = getNewSpot(nextSpot, 1);
        }
        return result.stream().map(spot -> spot.getTokenCoords(player)).collect(Collectors.toList());
    }

    public List<Coordinates> getPath(Spot start, Spot end, Player player, boolean flyOverSpots) {
        if (flyOverSpots) {
            List<Spot> result = Arrays.asList(start, end);
            return result.stream().map(spot -> spot.getTokenCoords(player)).collect(Collectors.toList());
        }
        return getPath(start, getDistance(start, end), player);
    }

    public List<Coordinates> getPath(Spot start, Spot end, Player player) {
        return getPath(start, end, player, false);
    }

    private int getDistance(Spot start, Spot end) {
        int result = 0;
        Spot nextSpot = start;
        while (!nextSpot.equals(end)) {
            result++;
            nextSpot = getNewSpot(nextSpot, 1);
        }
        return result;
    }

    public Spot getJailSpot() {
        return spots.get(10);
    }
}
