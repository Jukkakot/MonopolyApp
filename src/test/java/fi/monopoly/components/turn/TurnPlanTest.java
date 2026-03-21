package fi.monopoly.components.turn;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TurnPlanTest {

    @Test
    void ofCreatesPlanWithProvidedEffectsInOrder() {
        ShowDiceEffect first = new ShowDiceEffect();
        ShowEndTurnEffect second = new ShowEndTurnEffect();

        TurnPlan plan = TurnPlan.of(TurnPhase.WAITING_FOR_EXTRA_ROLL, first, second);

        assertEquals(TurnPhase.WAITING_FOR_EXTRA_ROLL, plan.phase());
        assertEquals(2, plan.effects().size());
        assertInstanceOf(ShowDiceEffect.class, plan.effects().get(0));
        assertInstanceOf(ShowEndTurnEffect.class, plan.effects().get(1));
    }
}
