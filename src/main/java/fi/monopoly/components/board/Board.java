package fi.monopoly.components.board;

import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.spots.SpotFactory;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.LayoutMetrics;
import lombok.Getter;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final MonopolyRuntime runtime;
    @Getter //For debugging
    private final List<Spot> spots = new ArrayList<>();
    private Spot cachedHoveredSpot;
    private PGraphics backgroundLayer;
    private int backgroundLayerWidth = -1;
    private int backgroundLayerHeight = -1;

    public Board(MonopolyRuntime runtime) {
        this.runtime = runtime;
        initSpots();
    }

    private void initSpots() {
        float currX = (Spot.SPOT_H + 9 * Spot.SPOT_W) + Spot.SPOT_H / 2;
        float currY = (Spot.SPOT_H + 9 * Spot.SPOT_W) + Spot.SPOT_H / 2;
        int currRotation = 0;

        //BOTTOM ROW
        spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
        currX -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currX -= Spot.SPOT_W;

        }
        currX += Spot.SPOT_W;
        currX -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));

        //LEFT COLUMN
        currRotation += 90;
        currY -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currY -= Spot.SPOT_W;
        }
        currY += Spot.SPOT_W;
        currY -= Spot.SPOT_H / 2 + Spot.SPOT_W / 2;

        //TOP ROW
        currRotation += 90;
        spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
        currX += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currX += Spot.SPOT_W;
        }
        currX -= Spot.SPOT_W;
        currX += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));

        //RIGHT COLUMN
        currRotation += 90;
        currY += Spot.SPOT_H / 2 + Spot.SPOT_W / 2;
        for (int i = 0; i < 9; i++) {
            spots.add(SpotFactory.getSpot(runtime, new Coordinates(currX, currY, currRotation), SpotType.SPOT_TYPES.get(spots.size())));
            currY += Spot.SPOT_W;
        }
    }

    public void draw(Coordinates c) {
        drawBackground();
        drawSpots(c);
    }

    private void drawSpots(Coordinates c) {
        Spot firstHoveredSpot = null;
        Spot secondHoveredSpot = null;
        for (Spot spot : spots) {
            if (!spot.getImage().isHovered()) {
                spot.getImage().draw(c);
                continue;
            }
            if (firstHoveredSpot == null) {
                firstHoveredSpot = spot;
            } else if (secondHoveredSpot == null) {
                secondHoveredSpot = spot;
            }
        }
        if (firstHoveredSpot != null) {
            firstHoveredSpot.getImage().draw(c);
        }
        if (secondHoveredSpot != null) {
            secondHoveredSpot.getImage().draw(c);
        }
        cachedHoveredSpot = secondHoveredSpot == null ? firstHoveredSpot : null;
    }

    public Spot getHoveredSpot() {
        Spot firstHoveredSpot = null;
        for (Spot spot : spots) {
            if (!spot.getImage().isHovered()) {
                continue;
            }
            if (firstHoveredSpot != null) {
                cachedHoveredSpot = null;
                return null;
            }
            firstHoveredSpot = spot;
        }
        cachedHoveredSpot = firstHoveredSpot;
        return cachedHoveredSpot;
    }

    private void drawBackground() {
        MonopolyApp p = runtime.app();
        LayoutMetrics layoutMetrics = LayoutMetrics.fromWindow(p.width, p.height);
        int boardWidth = Math.max(1, Math.round(layoutMetrics.boardWidth()));
        int boardHeight = Math.max(1, p.height);
        if (backgroundLayer == null || backgroundLayerWidth != boardWidth || backgroundLayerHeight != boardHeight) {
            backgroundLayerWidth = boardWidth;
            backgroundLayerHeight = boardHeight;
            backgroundLayer = p.createGraphics(boardWidth, boardHeight, MonopolyApp.FX2D);
            backgroundLayer.beginDraw();
            backgroundLayer.clear();
            backgroundLayer.imageMode(p.CENTER);
            float imgSize = Spot.SPOT_W * 9;
            float middleRadius = Spot.SPOT_W * 6;
            backgroundLayer.image(MonopolyApp.getImage("Background.png"), middleRadius, middleRadius, imgSize, imgSize);
            backgroundLayer.endDraw();
        }
        p.imageMode(p.CORNER);
        p.image(backgroundLayer, 0, 0);
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

    public Path getPath(Player turnPlayer, int value, PathMode pathMode) {
        Spot start = turnPlayer.getSpot();
        List<Spot> result = new ArrayList<>();
        Spot nextSpot = getNewSpot(start, 1, pathMode);
        for (int i = 0; i < value; i++) {
            result.add(nextSpot);
            nextSpot = getNewSpot(nextSpot, 1, pathMode);
        }
        return new Path(result, turnPlayer);
    }

    public Path getPath(Player turnPlayer, Spot end, PathMode pathMode) {
        Spot start = turnPlayer.getSpot();
        if (PathMode.FLY.equals(pathMode)) {
            List<Spot> result = new ArrayList<>();
            result.add(end);
            return new Path(result, turnPlayer);
        }
        return getPath(turnPlayer, getDistance(start, end, pathMode), pathMode);
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

    public Spot getPathWithCriteria(SpotType spotType) {
        for (Spot spot : spots) {
            if (spot.getSpotType().equals(spotType)) {
                return spot;
            }
        }
        throw new IllegalArgumentException("Spot type not found on board: " + spotType);
    }

    public Spot getNextSpot(StreetType streetType, Spot currSpot, PathMode pathMode) {
        Spot result = currSpot;
        while (!result.getSpotType().streetType.equals(streetType)) {
            result = getNewSpot(result, 1, pathMode);
        }
        return result;
    }

    public Path getPathWithCriteria(TurnResult turnResult, Player turnPlayer) {
        Spot currSpot = turnPlayer.getSpot();
        Object spotCriteria = turnResult.getNextSpotCriteria();
        PathMode pathMode = turnResult.getPathMode();
        Spot newSpot;
        if (spotCriteria instanceof SpotType spotType) {
            newSpot = getPathWithCriteria(spotType);
        } else if (spotCriteria instanceof StreetType streetType) {
            newSpot = getNextSpot(streetType, currSpot, pathMode);
        } else {
            newSpot = getNewSpot(currSpot, (int) spotCriteria, pathMode);
        }
        return getPath(turnPlayer, newSpot, pathMode);
    }
}
