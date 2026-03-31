package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TradeOfferTest {

    @Test
    void tradeOfferAppliesMoneyPropertyAndJailCardTransfers() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        Property offeredProperty = new StreetProperty(SpotType.B1);
        Property requestedProperty = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, offeredProperty);
        TestObjectFactory.giveProperty(recipient, requestedProperty);
        proposer.addOutOfJailCard();

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(100, java.util.List.of(offeredProperty), true),
                new TradeSelection(50, java.util.List.of(requestedProperty), false)
        );

        assertTrue(offer.isValid());
        assertTrue(offer.apply());
        assertEquals(450, proposer.getMoneyAmount());
        assertEquals(450, recipient.getMoneyAmount());
        assertEquals(recipient, offeredProperty.getOwnerPlayer());
        assertEquals(proposer, requestedProperty.getOwnerPlayer());
        assertEquals(0, proposer.getGetOutOfJailCardCount());
        assertEquals(1, recipient.getGetOutOfJailCardCount());
    }

    @Test
    void tradeOfferAppliesMultiplePropertyTransfers() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        Property b1 = new StreetProperty(SpotType.B1);
        Property b2 = new StreetProperty(SpotType.B2);
        Property o1 = new StreetProperty(SpotType.O1);
        TestObjectFactory.giveProperty(proposer, b1);
        TestObjectFactory.giveProperty(proposer, b2);
        TestObjectFactory.giveProperty(recipient, o1);

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, java.util.List.of(b1, b2), false),
                new TradeSelection(0, java.util.List.of(o1), false)
        );

        assertTrue(offer.isValid());
        assertTrue(offer.apply());
        assertEquals(recipient, b1.getOwnerPlayer());
        assertEquals(recipient, b2.getOwnerPlayer());
        assertEquals(proposer, o1.getOwnerPlayer());
    }

    @Test
    void tradeOfferRejectsBuiltStreetProperties() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        StreetProperty offeredProperty = new StreetProperty(SpotType.B1);
        StreetProperty setMate = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, offeredProperty);
        TestObjectFactory.giveProperty(proposer, setMate);
        assertTrue(offeredProperty.buyHouses(1));

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, java.util.List.of(offeredProperty), false),
                TradeSelection.NONE
        );

        assertFalse(offer.isValid());
    }
}
