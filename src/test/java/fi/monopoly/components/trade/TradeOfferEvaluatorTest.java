package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
                new TradeSelection(0, java.util.List.of(b2), false),
                new TradeSelection(50, java.util.List.of(), false)
        ), BotTradeProfile.BALANCED);

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
                new TradeSelection(50, java.util.List.of(), false),
                new TradeSelection(0, java.util.List.of(b1), false)
        ), BotTradeProfile.CAUTIOUS);

        assertFalse(decision.accept());
    }

    @Test
    void profilesUseDifferentAcceptanceThresholds() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(30, java.util.List.of(), false),
                TradeSelection.NONE
        );

        assertFalse(evaluator.evaluateForRecipient(offer, BotTradeProfile.CAUTIOUS).accept());
        assertTrue(evaluator.evaluateForRecipient(offer, BotTradeProfile.BALANCED).accept());
        assertTrue(evaluator.evaluateForRecipient(offer, BotTradeProfile.AGGRESSIVE).accept());
    }

    @Test
    void counterOfferRaisesMoneyToMeetProfileThreshold() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(30, java.util.List.of(), false),
                TradeSelection.NONE
        );

        TradeOffer counterOffer = evaluator.proposeCounterOffer(offer, BotTradeProfile.CAUTIOUS);

        assertTrue(counterOffer != null);
        assertTrue(counterOffer.offeredToRecipient().moneyAmount() >= 60);
        assertTrue(evaluator.evaluateForRecipient(counterOffer, BotTradeProfile.CAUTIOUS).accept());
    }

    @Test
    void counterOfferIsSkippedWhenTradeIsFarTooOneSided() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(recipient, b1);
        TestObjectFactory.giveProperty(recipient, b2);

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                TradeSelection.NONE,
                new TradeSelection(0, java.util.List.of(b1, b2), false)
        );

        assertNull(evaluator.proposeCounterOffer(offer, BotTradeProfile.CAUTIOUS));
        assertFalse(evaluator.evaluateForRecipient(offer, BotTradeProfile.CAUTIOUS).accept());
    }

    @Test
    void evaluatorValuesMultipleIncomingProperties() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, b1);
        TestObjectFactory.giveProperty(proposer, b2);

        TradeDecision decision = evaluator.evaluateForRecipient(new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, java.util.List.of(b1, b2), false),
                TradeSelection.NONE
        ), BotTradeProfile.BALANCED);

        assertTrue(decision.accept());
    }
}
