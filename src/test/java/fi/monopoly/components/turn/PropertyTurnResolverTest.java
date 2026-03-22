package fi.monopoly.components.turn;

import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyTurnResolverTest {

    private final PropertyTurnResolver resolver = new PropertyTurnResolver();

    @Test
    void resolveReturnsOfferToBuyWhenPropertyHasNoOwner() {
        Player turnPlayer = TestObjectFactory.player("Turn", 1500, 1);
        Players players = TestObjectFactory.playersWithTurn(turnPlayer);
        StreetProperty property = new StreetProperty(SpotType.B1);

        List<TurnEffect> effects = resolver.resolve(new GameState(players, null, null, null, null), "Brown 1", property);

        assertEquals(1, effects.size());
        OfferToBuyPropertyEffect effect = (OfferToBuyPropertyEffect) effects.get(0);
        assertEquals(turnPlayer, effect.player());
        assertEquals(property, effect.property());
        assertTrue(effect.message().contains("Brown 1"));
        assertTrue(effect.message().contains("M60"));
    }

    @Test
    void resolveReturnsRentPaymentWhenOwnedByOtherPlayer() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player turnPlayer = TestObjectFactory.player("Turn", 1500, 2);
        Players players = TestObjectFactory.playersWithTurn(turnPlayer, owner);
        StreetProperty property = new StreetProperty(SpotType.B1);
        StreetProperty sibling = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(owner, property);
        TestObjectFactory.giveProperty(owner, sibling);

        List<TurnEffect> effects = resolver.resolve(new GameState(players, null, null, null, null), "Brown 1", property);

        assertEquals(1, effects.size());
        PayRentEffect effect = (PayRentEffect) effects.get(0);
        assertEquals(turnPlayer, effect.fromPlayer());
        assertEquals(owner, effect.toPlayer());
        assertEquals(4, effect.amount());
    }

    @Test
    void resolveReturnsNoEffectsForMortgagedProperty() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player turnPlayer = TestObjectFactory.player("Turn", 1500, 2);
        Players players = TestObjectFactory.playersWithTurn(turnPlayer, owner);
        StreetProperty property = new StreetProperty(SpotType.B1);
        TestObjectFactory.giveProperty(owner, property);
        property.setMortgaged(true);

        List<TurnEffect> effects = resolver.resolve(new GameState(players, null, null, null, null), "Brown 1", property);

        assertTrue(effects.isEmpty());
    }
}
