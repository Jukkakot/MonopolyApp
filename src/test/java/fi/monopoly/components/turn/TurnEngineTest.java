package fi.monopoly.components.turn;

import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.board.Path;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnEngineTest {

    private final TurnEngine turnEngine = new TurnEngine();

    @Test
    void createFollowUpPlanShowsDiceAfterDoublesWhenNoPendingTurnResult() {
        TurnPlan plan = turnEngine.createFollowUpPlan(null, null, null, DiceState.DOUBLES);

        assertEquals(TurnPhase.WAITING_FOR_EXTRA_ROLL, plan.phase());
        assertInstanceOf(ShowDiceEffect.class, plan.effects().get(0));
    }

    @Test
    void createFollowUpPlanShowsEndTurnWhenNoPendingEffectsRemain() {
        TurnPlan plan = turnEngine.createFollowUpPlan(null, null, null, DiceState.NOREROLL);

        assertEquals(TurnPhase.WAITING_TO_END_TURN, plan.phase());
        assertInstanceOf(ShowEndTurnEffect.class, plan.effects().get(0));
    }

    @Test
    void endTurnCreatesTurnFinishedPlan() {
        TurnPlan plan = turnEngine.endTurn(true);

        assertEquals(TurnPhase.TURN_FINISHED, plan.phase());
        EndTurnEffect effect = (EndTurnEffect) plan.effects().get(0);
        assertTrue(effect.switchTurns());
    }

    @Test
    void turnResultCanCarryFollowUpMovementData() {
        TurnResult result = TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .build();

        assertEquals(SpotType.JAIL, result.getNextSpotCriteria());
        assertEquals(PathMode.FLY, result.getPathMode());
    }

    @Test
    void createFollowUpPlanCreatesMovementPlanWhenTurnResultExists() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Board board = new Board(runtime);
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));
        TurnResult turnResult = TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .build();

        TurnPlan plan = turnEngine.createFollowUpPlan(player, board, turnResult, DiceState.NOREROLL);

        assertEquals(TurnPhase.MOVING, plan.phase());
        MovePlayerEffect effect = (MovePlayerEffect) plan.effects().get(0);
        assertSame(SpotType.JAIL, effect.path().getLastSpot().getSpotType());
        assertEquals(DiceState.NOREROLL, effect.diceState());
    }

    @Test
    void createMovementPlanUsesFlyPathWhenDiceStateSendsPlayerToJail() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Board board = new Board(runtime);
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));

        TurnPlan plan = turnEngine.createMovementPlan(player, board, new DiceValue(DiceState.JAIL, 12));

        assertEquals(TurnPhase.MOVING, plan.phase());
        MovePlayerEffect effect = (MovePlayerEffect) plan.effects().get(0);
        assertEquals(DiceState.JAIL, effect.diceState());
        assertEquals(SpotType.JAIL, effect.path().getLastSpot().getSpotType());
        Path path = effect.path();
        path.removePrevious();
        assertTrue(path.isEmpty());
    }

    @Test
    void createMovementPlanUsesNormalPathForRegularRoll() {
        new MonopolyApp();
        MonopolyRuntime runtime = MonopolyRuntime.get();
        Board board = new Board(runtime);
        Player player = new Player(runtime, "Player", Color.BLACK, board.getSpots().get(0));

        TurnPlan plan = turnEngine.createMovementPlan(player, board, new DiceValue(DiceState.NOREROLL, 3));

        MovePlayerEffect effect = (MovePlayerEffect) plan.effects().get(0);
        assertEquals(SpotType.B2, effect.path().getLastSpot().getSpotType());
        Path path = effect.path();
        path.removePrevious();
        assertFalse(path.isEmpty());
        path.removePrevious();
        assertFalse(path.isEmpty());
        path.removePrevious();
        assertTrue(path.isEmpty());
    }
}
