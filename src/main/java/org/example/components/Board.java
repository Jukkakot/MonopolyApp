package org.example.components;

import lombok.Getter;
import org.example.MonopolyApp;
import org.example.components.spots.Spot;
import org.example.components.spots.SpotFactory;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
