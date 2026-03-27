package fi.monopoly.components;

import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

    @Test
    void totalLiquidationValueIncludesStreetBuildingSellValue() {
        Player owner = TestObjectFactory.player("Owner", 2_000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertTrue(first.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(first.buyHouses(1));

        int expectedLiquidationValue = first.getLiquidationValue() + second.getLiquidationValue();
        assertEquals(expectedLiquidationValue, owner.getTotalLiquidationValue());
        assertEquals(135, owner.getTotalLiquidationValue());
    }
}
