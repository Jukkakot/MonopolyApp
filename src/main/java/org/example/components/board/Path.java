package org.example.components.board;

import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.utils.Coordinates;

import java.util.List;
import java.util.stream.Collectors;

public class Path {
    final List<Spot> spots;
    List<Coordinates> waypoints;

    public Path(List<Spot> spots) {
        this.spots = spots;
        waypoints = spots.stream().map(Spot::getTokenCoords).collect(Collectors.toList());
    }

    public boolean remove(Coordinates coordinates) {
        return waypoints.remove(coordinates);
    }

    public boolean isEmpty() {
        return waypoints.isEmpty();
    }

    public Coordinates getLast() {
        return waypoints.get(waypoints.size() - 1);
    }

    public Coordinates getNext() {
        return waypoints.get(0);
    }

    public Spot getLastSpot() {
        return spots.get(spots.size() - 1);
    }

    private Spot getFirstSpot() {
        return spots.get(0);
    }

    public boolean containsGoSpot() {
        return contains(SpotType.GO_SPOT) && getFirstSpot().getSpotType() != SpotType.GO_SPOT;
    }

    private boolean contains(SpotType spotType) {
        return spots.stream().anyMatch(spot -> spotType.equals(spot.getSpotType()));
    }
}
