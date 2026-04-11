package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerAction;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.StrongBotConfig;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongTradePlannerTest {

    private final StrongTradePlanner planner = new StrongTradePlanner(StrongBotConfig.defaults());

    @Test
    void plannerProposesCashTradeThatCompletesSet() {
        Player proposer = new Player("Bot", Color.BLACK, 900, 1, ComputerPlayerProfile.STRONG);
        Player recipient = new Player("Other", Color.BLUE, 1500, 2, ComputerPlayerProfile.SMOKE_TEST);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, b1);
        TestObjectFactory.giveProperty(recipient, b2);

        StrongTradePlanner.TradePlan plan = planner.plan(proposer, List.of(proposer, recipient));

        assertNotNull(plan);
        assertEquals(ComputerAction.PROPOSE_TRADE, plan.decision().action());
        assertEquals(proposer, plan.offer().proposer());
        assertEquals(recipient, plan.offer().recipient());
        assertEquals(List.of(b2), plan.offer().requestedFromRecipient().properties());
        assertTrue(plan.offer().offeredToRecipient().moneyAmount() > 0);
        assertTrue(plan.decision().reason().contains("completes BROWN"));
    }
}
