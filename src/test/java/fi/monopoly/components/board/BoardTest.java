package fi.monopoly.components.board;

import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private MonopolyRuntime runtime;
    private Board board;

    @BeforeEach
    void setUp() {
        new MonopolyApp();
        runtime = MonopolyRuntime.get();
        board = new Board(runtime);
    }

    @Test
    void getNewSpotWrapsAroundBoardInNormalMode() {
        Spot start = board.getSpots().get(board.getSpots().size() - 2);

        Spot result = board.getNewSpot(start, 3, PathMode.NORMAL);

        assertSame(board.getSpots().get(1), result);
    }

    @Test
    void getNewSpotMovesBackwardsAndWrapsWhenNeeded() {
        Spot start = board.getSpots().get(1);

        Spot result = board.getNewSpot(start, 3, PathMode.BACKWARDS);

        assertSame(board.getSpots().get(38), result);
    }

    @Test
    void getPathBuildsSequentialRouteForDiceValue() {
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));

        Path path = board.getPath(player, 3, PathMode.NORMAL);

        assertEquals(SpotType.B1, path.spots.get(0).getSpotType());
        assertEquals(SpotType.COMMUNITY1, path.spots.get(1).getSpotType());
        assertEquals(SpotType.B2, path.spots.get(2).getSpotType());
        assertEquals(SpotType.B2, path.getLastSpot().getSpotType());
    }

    @Test
    void getPathReturnsSingleStepInFlyMode() {
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));
        Spot end = board.getSpots().get(10);

        Path path = board.getPath(player, end, PathMode.FLY);

        assertEquals(1, path.spots.size());
        assertSame(end, path.getLastSpot());
    }

    @Test
    void getPathWithCriteriaSupportsSpotTypeStreetTypeAndStepCount() {
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));

        Path toSpotType = board.getPathWithCriteria(TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.NORMAL)
                .build(), player);
        Path toStreetType = board.getPathWithCriteria(TurnResult.builder()
                .nextSpotCriteria(StreetType.RAILROAD)
                .pathMode(PathMode.NORMAL)
                .build(), player);
        Path byStepCount = board.getPathWithCriteria(TurnResult.builder()
                .nextSpotCriteria(3)
                .pathMode(PathMode.NORMAL)
                .build(), player);

        assertEquals(SpotType.JAIL, toSpotType.getLastSpot().getSpotType());
        assertEquals(toStreetType.getLastSpot().getSpotType().streetType, StreetType.RAILROAD);
        assertEquals(SpotType.B2, byStepCount.getLastSpot().getSpotType());
    }

    @Test
    void getHoveredSpotReturnsOnlySingleHoveredSpot() {
        Spot first = board.getSpots().get(0);
        Spot second = board.getSpots().get(1);

        first.getImage().setHovered(true);
        assertSame(first, board.getHoveredSpot());

        second.getImage().setHovered(true);
        assertNull(board.getHoveredSpot());

        first.getImage().setHovered(false);
        second.getImage().setHovered(false);
        assertNull(board.getHoveredSpot());
    }
}
