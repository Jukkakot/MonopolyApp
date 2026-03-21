package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilityPropertyTest {

    @Test
    void multiplierRentUsesDiceValue() {
        UtilityProperty utility = new UtilityProperty(SpotType.U1);

        assertEquals(70, utility.getMultiplierRent(7));
    }

    @Test
    void rentUsesFourTimesDiceWithOneUtilityAndTenTimesWithBoth() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player visitor = TestObjectFactory.player("Visitor", 1500, 2);
        UtilityProperty first = new UtilityProperty(SpotType.U1);
        UtilityProperty second = new UtilityProperty(SpotType.U2);

        TestObjectFactory.giveProperty(owner, first);
        assertEquals(32, first.getRentForDiceValue(visitor, 8));

        TestObjectFactory.giveProperty(owner, second);
        assertEquals(80, first.getRentForDiceValue(visitor, 8));
    }
}
