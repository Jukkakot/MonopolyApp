package fi.monopoly.components.board;

import fi.monopoly.components.animation.AnimationPath;
import fi.monopoly.components.Player;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Path implements AnimationPath {
    final List<Spot> spots;
    final Player player;
    @Getter
    final Spot lastSpot;

    public Path(List<Spot> spots, Player player) {
        this.spots = spots;
        this.player = player;
        this.lastSpot = spots.get(spots.size() - 1);
    }

    public void removePrevious() {
        if (spots == null || spots.isEmpty()) {
            return;
        }
        spots.remove(0);
    }

    public boolean isEmpty() {
        return spots.isEmpty();
    }

    public Coordinates getLast() {
        return this.lastSpot.getTokenCoords(player);
    }

    public Coordinates getNext() {
        return getNextSpot().getTokenCoords(player);
    }

    private Spot getNextSpot() {
        return spots.get(0);
    }

    public boolean passesGoSpot() {
        return spots.stream().anyMatch(spot -> spot.isSpotType(SpotType.GO_SPOT));
    }
}
