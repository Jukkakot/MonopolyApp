package org.example.components.board;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.components.spots.SpotFactory;
import org.example.types.PathMode;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.*;

public class Board {
    @Getter
    private final List<Spot> spots = new ArrayList<>();
    MonopolyApp p = MonopolyApp.self;

    public Board() {
        initSpots();
    }

    private void initSpots() {
        float currX = (Spot.SPOT_H + 9 * Spot.SPOT_W) + Spot.SPOT_H / 2;
        float currY = (Spot.SPOT_H + 9 * Spot.SPOT_W) + Spot.SPOT_H / 2;
        int currRotation = 0;

        //BOTTOM ROW
        spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
        currX -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currX -= Spot.SPOT_W;

        }
        currX += Spot.SPOT_W;
        currX -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));

        //LEFT COLUMN
        currRotation += 90;
        currY -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currY -= Spot.SPOT_W;
        }
        currY += Spot.SPOT_W;
        currY -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;

        //TOP ROW
        currRotation += 90;
        spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
        currX += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currX += Spot.SPOT_W;
        }
        currX -= Spot.SPOT_W;
        currX += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));

        //RIGHT COLUMN
        currRotation += 90;
        currY += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currY += Spot.SPOT_W;
        }
    }

    public void draw(Coordinates c) {
        drawBackground();
        drawSpots(c);
    }

    private void drawSpots(Coordinates c) {
        // not hovered spots
        spots.stream().filter(spot -> !spot.isHovered()).forEach(spot -> spot.draw(c));
        // hovered spots
        spots.stream().filter(Spot::isHovered).forEach(spot -> spot.draw(c));
    }

    public Spot getHoveredSpot() {
        List<Spot> hoveredSpots = spots.stream().filter(Spot::isHovered).toList();
        if (hoveredSpots.size() == 1) {
            return hoveredSpots.get(0);
        } else {
            return null;
        }
    }

    private void drawBackground() {
        p.push();
        p.imageMode(p.CENTER);
        float imgSize = Spot.SPOT_W * 9;
        float middleRadius = Spot.SPOT_W * 6;
        p.image(MonopolyApp.getImage("Background.png"), middleRadius, middleRadius, imgSize, imgSize);
        p.pop();
    }

    public Spot getNewSpot(Spot spot, int diceValue, PathMode pathMode) {
        int currSpot = spots.indexOf(spot);
        int nextSpotIndex = currSpot + diceValue;
        if (PathMode.BACKWARDS.equals(pathMode)) {
            nextSpotIndex = currSpot - diceValue;
        }
        int index = nextSpotIndex < 0 ? spots.size() + nextSpotIndex : nextSpotIndex;
        return spots.get(index % spots.size());
    }

    public Path getPath(Spot start, int value, PathMode pathMode) {
        List<Spot> result = new ArrayList<>();
        Spot nextSpot = getNewSpot(start, 1, pathMode);
        for (int i = 0; i < value; i++) {
            result.add(nextSpot);
            nextSpot = getNewSpot(nextSpot, 1, pathMode);
        }
        return new Path(result);
    }

    public Path getPath(Spot start, Spot end, PathMode pathMode) {
        if (PathMode.FLY.equals(pathMode)) {
            List<Spot> result = Collections.singletonList(end);
            return new Path(result);
        }
        return getPath(start, getDistance(start, end, pathMode), pathMode);
    }

    private int getDistance(Spot start, Spot end, PathMode pathMode) {
        int startIndex = spots.indexOf(start);
        int endIndex = spots.indexOf(end);
        int distance = endIndex - startIndex;
        if (pathMode.equals(PathMode.BACKWARDS)) {
            distance = startIndex - endIndex;
        }
        distance = distance < 0 ? spots.size() + distance : distance;
        return distance % spots.size();
    }

    public Spot getSpot(SpotType spotType) {
        return spots.stream().filter(spot -> spot.getSpotType().equals(spotType)).toList().get(0);
    }
}
