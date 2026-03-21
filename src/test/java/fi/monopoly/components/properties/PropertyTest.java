package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTest {

    @Test
    void ownerHelpersReflectOwnershipState() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player other = TestObjectFactory.player("Other", 1500, 2);
        Property property = new StreetProperty(SpotType.B1);

        assertFalse(property.hasOwner());
        assertFalse(property.isOwner(owner));
        assertTrue(property.isNotOwner(owner));

        property.setOwnerPlayer(owner);

        assertTrue(property.hasOwner());
        assertTrue(property.isOwner(owner));
        assertFalse(property.isNotOwner(owner));
        assertTrue(property.isNotOwner(other));
    }

    @Test
    void sameStreetTypeCheckUsesSpotTypeStreetType() {
        Property property = new StreetProperty(SpotType.B1);

        assertTrue(property.isSameStreetType(StreetType.BROWN));
        assertFalse(property.isSameStreetType(StreetType.RED));
    }

    @Test
    void handleMortgagingSetsMortgageAndAddsHalfPriceToOwner() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        Property property = new StreetProperty(SpotType.B1);
        property.setOwnerPlayer(owner);

        boolean result = property.handleMortgaging();

        assertTrue(result);
        assertTrue(property.isMortgaged());
        assertEquals(1030, owner.getMoneyAmounnt());
    }

    @Test
    void handleMortgagingRemovesMortgageWhenOwnerCanPayInterest() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        Property property = new StreetProperty(SpotType.B1);
        property.setOwnerPlayer(owner);
        property.setMortgaged(true);

        boolean result = property.handleMortgaging();

        assertTrue(result);
        assertFalse(property.isMortgaged());
        assertEquals(967, owner.getMoneyAmounnt());
    }

    @Test
    void handleMortgagingFailsWhenNoOwnerExists() {
        Property property = new StreetProperty(SpotType.B1);

        boolean result = property.handleMortgaging();

        assertFalse(result);
        assertFalse(property.isMortgaged());
    }

    @Test
    void handleMortgagingFailsWhenOwnerCannotAffordUnmortgage() {
        Player owner = TestObjectFactory.player("Owner", 10, 1);
        Property property = new StreetProperty(SpotType.B1);
        property.setOwnerPlayer(owner);
        property.setMortgaged(true);

        boolean result = property.handleMortgaging();

        assertFalse(result);
        assertTrue(property.isMortgaged());
        assertEquals(10, owner.getMoneyAmounnt());
    }
}
