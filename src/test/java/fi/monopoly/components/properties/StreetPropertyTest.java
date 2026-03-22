package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StreetPropertyTest {

    @Test
    void rentIsDoubledWhenOwnerHasFullColorSetWithoutBuildings() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player visitor = TestObjectFactory.player("Visitor", 1500, 2);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertEquals(4, first.getRent(visitor));
    }

    @Test
    void rentUsesBuildingLevelWhenHousesOrHotelExist() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player visitor = TestObjectFactory.player("Visitor", 1500, 2);
        StreetProperty property = new StreetProperty(SpotType.B1);

        TestObjectFactory.giveProperty(owner, property);

        assertTrue(property.buyHouses(2));
        assertEquals(30, property.getRent(visitor));

        assertTrue(property.buyHouses(3));
        assertEquals(250, property.getRent(visitor));
        assertEquals(0, property.getHouseCount());
        assertEquals(1, property.getHotelCount());
    }

    @Test
    void buyingAndSellingHousesUpdatesOwnerMoneyAndBuildingCounts() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);

        TestObjectFactory.giveProperty(owner, property);

        assertTrue(property.buyHouses(1));
        assertEquals(950, owner.getMoneyAmount());
        assertEquals(1, property.getHouseCount());

        assertTrue(property.sellHouses(1));
        assertEquals(975, owner.getMoneyAmount());
        assertEquals(0, property.getHouseCount());
    }

    @Test
    void sellableBuildingCountTreatsHotelAsFiveHouses() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);

        TestObjectFactory.giveProperty(owner, property);

        assertEquals(0, property.getSellableBuildingCount());

        assertTrue(property.buyHouses(2));
        assertEquals(2, property.getSellableBuildingCount());

        assertTrue(property.buyHouses(3));
        assertEquals(5, property.getSellableBuildingCount());
    }

    @Test
    void hasBuildingsReflectsCurrentBuildingState() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        TestObjectFactory.giveProperty(owner, property);

        assertFalse(property.hasBuildings());
        property.buyHouses(1);
        assertTrue(property.hasBuildings());
    }
}
