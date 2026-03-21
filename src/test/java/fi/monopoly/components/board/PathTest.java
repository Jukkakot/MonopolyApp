package fi.monopoly.components.board;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.spots.CornerSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathTest {

    @Test
    void getNextAndGetLastUseCurrentAndLastSpotTokenCoordinates() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Player player = new Player("Player", Color.BLACK, 1500, 1);
        Spot first = spot(runtime, SpotType.GO_SPOT, new Coordinates(10, 20, 0));
        Spot last = spot(runtime, SpotType.JAIL, new Coordinates(30, 40, 0));
        Path path = new Path(new ArrayList<>(List.of(first, last)), player);

        assertEquals(new Coordinates(10, 20, 0), path.getNext());
        assertEquals(new Coordinates(30, 40, 0), path.getLast());
    }

    @Test
    void removePreviousDropsFirstStepAndEventuallyEmptiesPath() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Player player = new Player("Player", Color.BLACK, 1500, 1);
        Spot first = spot(runtime, SpotType.GO_SPOT, new Coordinates(10, 20, 0));
        Spot second = spot(runtime, SpotType.JAIL, new Coordinates(30, 40, 0));
        Path path = new Path(new ArrayList<>(List.of(first, second)), player);

        path.removePrevious();
        assertEquals(new Coordinates(30, 40, 0), path.getNext());
        assertFalse(path.isEmpty());

        path.removePrevious();
        assertTrue(path.isEmpty());

        path.removePrevious();
        assertTrue(path.isEmpty());
    }

    @Test
    void passesGoSpotReturnsTrueOnlyWhenGoIsIncludedInPath() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Player player = new Player("Player", Color.BLACK, 1500, 1);
        Path withGo = new Path(new ArrayList<>(List.of(
                spot(runtime, SpotType.B1, new Coordinates(10, 20, 0)),
                spot(runtime, SpotType.GO_SPOT, new Coordinates(30, 40, 0))
        )), player);
        Path withoutGo = new Path(new ArrayList<>(List.of(
                spot(runtime, SpotType.B1, new Coordinates(10, 20, 0)),
                spot(runtime, SpotType.JAIL, new Coordinates(30, 40, 0))
        )), player);

        assertTrue(withGo.passesGoSpot());
        assertFalse(withoutGo.passesGoSpot());
    }

    private static Spot spot(MonopolyRuntime runtime, SpotType spotType, Coordinates coords) {
        return new CornerSpot(new SpotImage(runtime, coords, spotType));
    }
}
