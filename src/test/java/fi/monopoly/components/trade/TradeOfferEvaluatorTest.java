package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeOfferEvaluatorTest {
    private final TradeOfferEvaluator evaluator = new TradeOfferEvaluator();

    @Test
    void evaluatorAcceptsTradeThatCompletesRecipientSet() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, b2);
        TestObjectFactory.giveProperty(recipient, b1);

        TradeDecision decision = evaluator.evaluateForRecipient(new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, b2, false),
                new TradeSelection(50, null, false)
        ));

        assertTrue(decision.accept());
    }

    @Test
    void evaluatorRejectsTradeThatGivesAwayCompletedSetForSmallCash() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        StreetProperty o1 = new StreetProperty(SpotType.O1);
        TestObjectFactory.giveProperty(recipient, b1);
        TestObjectFactory.giveProperty(recipient, b2);
        TestObjectFactory.giveProperty(proposer, o1);

        TradeDecision decision = evaluator.evaluateForRecipient(new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(50, null, false),
                new TradeSelection(0, b1, false)
        ));

        assertFalse(decision.accept());
    }
}
