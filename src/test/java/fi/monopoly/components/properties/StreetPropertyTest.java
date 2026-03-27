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
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, property);
        TestObjectFactory.giveProperty(owner, second);

        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertEquals(30, property.getRent(visitor));

        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertEquals(250, property.getRent(visitor));
        assertEquals(0, property.getHouseCount());
        assertEquals(1, property.getHotelCount());
    }

    @Test
    void buyingAndSellingHousesUpdatesOwnerMoneyAndBuildingCounts() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, property);
        TestObjectFactory.giveProperty(owner, second);

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
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, property);
        TestObjectFactory.giveProperty(owner, second);

        assertEquals(0, property.getSellableBuildingCount());

        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertEquals(2, property.getSellableBuildingCount());

        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(property.buyHouses(1));
        assertEquals(5, property.getSellableBuildingCount());
    }

    @Test
    void hasBuildingsReflectsCurrentBuildingState() {
        Player owner = TestObjectFactory.player("Owner", 1000, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(owner, property);
        TestObjectFactory.giveProperty(owner, second);

        assertFalse(property.hasBuildings());
        assertTrue(property.buyHouses(1));
        assertTrue(property.hasBuildings());
    }

    @Test
    void buyingHousesMustFollowEvenBuildingRuleAcrossSet() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertTrue(first.buyHouses(1));
        assertFalse(first.buyHouses(1));
        assertEquals(1, first.getHouseCount());
        assertEquals(0, second.getHouseCount());

        assertTrue(second.buyHouses(1));
        assertTrue(first.buyHouses(1));
        assertEquals(2, first.getHouseCount());
        assertEquals(1, second.getHouseCount());
    }

    @Test
    void sellingHousesMustFollowEvenBuildingRuleAcrossSet() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertTrue(first.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(first.buyHouses(1));

        assertFalse(second.sellHouses(1));
        assertEquals(2, first.getHouseCount());
        assertEquals(1, second.getHouseCount());

        assertTrue(first.sellHouses(1));
        assertEquals(1, first.getHouseCount());
        assertEquals(1, second.getHouseCount());
    }

    @Test
    void maxBuyableAndSellableCountsRespectEvenBuildingRule() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertEquals(1, first.getMaxBuyableHouseCount());
        assertTrue(first.buyHouses(1));
        assertEquals(0, first.getMaxBuyableHouseCount());
        assertEquals(0, second.getMaxSellableHouseCount());
        assertEquals(1, first.getMaxSellableHouseCount());
    }

    @Test
    void buyingBuildingRoundsAcrossSetBuildsAllPropertiesEvenly() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertEquals(5, first.getMaxBuyableBuildingRoundsAcrossSet());
        assertEquals(100, first.getStreetSetRoundCost(1));
        assertTrue(first.buyBuildingRoundsAcrossSet(2));

        assertEquals(2, first.getHouseCount());
        assertEquals(2, second.getHouseCount());
        assertEquals(1800, owner.getMoneyAmount());
    }

    @Test
    void buyingBuildingRoundsAcrossSetStopsAtAvailableMoney() {
        Player owner = TestObjectFactory.player("Owner", 260, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertEquals(2, first.getMaxBuyableBuildingRoundsAcrossSet());
        assertFalse(first.buyBuildingRoundsAcrossSet(3));
        assertTrue(first.buyBuildingRoundsAcrossSet(2));
        assertEquals(2, first.getHouseCount());
        assertEquals(2, second.getHouseCount());
        assertEquals(60, owner.getMoneyAmount());
    }
}
