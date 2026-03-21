package fi.monopoly.components.turn;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TurnEffectsRecordTest {

    @Test
    void adjustPlayerMoneyEffectExposesGivenValues() {
        Player player = TestObjectFactory.player("Owner", 1000, 1);
        AdjustPlayerMoneyEffect effect = new AdjustPlayerMoneyEffect(player, -50, "Pay tax");

        assertEquals(player, effect.player());
        assertEquals(-50, effect.amount());
        assertEquals("Pay tax", effect.message());
    }

    @Test
    void offerToBuyPropertyEffectExposesGivenValues() {
        Player player = TestObjectFactory.player("Owner", 1000, 1);
        Property property = new StreetProperty(SpotType.B1);
        OfferToBuyPropertyEffect effect = new OfferToBuyPropertyEffect(player, property, "Buy?");

        assertEquals(player, effect.player());
        assertEquals(property, effect.property());
        assertEquals("Buy?", effect.message());
    }

    @Test
    void payRentEffectExposesGivenValues() {
        Player from = TestObjectFactory.player("From", 1000, 1);
        Player to = TestObjectFactory.player("To", 1000, 2);
        PayRentEffect effect = new PayRentEffect(from, to, 25, "Pay rent");

        assertEquals(from, effect.fromPlayer());
        assertEquals(to, effect.toPlayer());
        assertEquals(25, effect.amount());
        assertEquals("Pay rent", effect.message());
    }

    @Test
    void showMessageEffectExposesMessage() {
        ShowMessageEffect effect = new ShowMessageEffect("Hello");

        assertEquals("Hello", effect.message());
    }

    @Test
    void movePlayerEffectCanCarryPathAndDiceState() {
        MovePlayerEffect effect = new MovePlayerEffect(null, DiceState.DEBUG_REROLL);

        assertNull(effect.path());
        assertEquals(DiceState.DEBUG_REROLL, effect.diceState());
    }
}
