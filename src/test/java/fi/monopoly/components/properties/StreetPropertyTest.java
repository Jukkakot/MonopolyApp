package fi.monopoly.components.properties;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Game;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.Player;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StreetPropertyTest {

    @BeforeEach
    void resetRuntimeSessionBeforeTest() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime != null) {
            runtime.setGameSession(null);
        }
    }

    @AfterEach
    void clearGamePlayers() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime != null) {
            runtime.setGameSession(null);
        }
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = MonopolyApp.DEFAULT_WINDOW_WIDTH;
        app.height = MonopolyApp.DEFAULT_WINDOW_HEIGHT;
        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;
        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

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

    @Test
    void sellingBuildingRoundsAcrossSetSellsHighestBuildingsEvenly() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        assertTrue(first.buyHouses(1));
        assertTrue(second.buyHouses(1));
        assertTrue(first.buyHouses(1));

        assertEquals(2, first.getMaxSellableBuildingRoundsAcrossSet());
        assertTrue(first.sellBuildingRoundsAcrossSet(1));
        assertEquals(1, first.getHouseCount());
        assertEquals(0, second.getHouseCount());
        assertEquals(1900, owner.getMoneyAmount());
    }

    @Test
    void sellingBuildingRoundsAcrossSetCanBreakHotelAcrossSet() {
        Player owner = TestObjectFactory.player("Owner", 5000, 1);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);

        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);
        setBuildingState(first, 0, 1);
        setBuildingState(second, 0, 1);

        assertEquals(5, first.getMaxSellableBuildingRoundsAcrossSet());
        assertTrue(first.sellBuildingRoundsAcrossSet(1));
        assertEquals(4, first.getHouseCount());
        assertEquals(0, first.getHotelCount());
        assertEquals(4, second.getHouseCount());
        assertEquals(0, second.getHotelCount());
        assertEquals(5050, owner.getMoneyAmount());
    }

    @Test
    void buyingHouseFailsWhenBankHasNoHousesLeft() {
        Player owner = TestObjectFactory.player("Owner", 2000, 1);
        Player other = TestObjectFactory.player("Other", 2000, 2);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);

        StreetProperty fillerA = new StreetProperty(SpotType.LB1);
        StreetProperty fillerB = new StreetProperty(SpotType.LB2);
        TestObjectFactory.giveProperty(other, fillerA);
        TestObjectFactory.giveProperty(other, fillerB);
        setBuildingState(fillerA, 16, 0);
        setBuildingState(fillerB, 16, 0);

        MonopolyRuntime runtime = initHeadlessRuntime();
        runtime.setGameSession(new GameSession(TestObjectFactory.playersWithTurn(owner, other), null, null));

        assertFalse(first.buyHouses(1));
        assertEquals(0, first.getHouseCount());
    }

    @Test
    void buyingHotelFailsWhenBankHasNoHotelsLeft() {
        Player owner = TestObjectFactory.player("Owner", 5000, 1);
        Player hotelOwner = TestObjectFactory.player("HotelOwner", 5000, 2);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);
        setBuildingState(first, 4, 0);
        setBuildingState(second, 4, 0);

        List<StreetProperty> hotelProperties = List.of(
                new StreetProperty(SpotType.LB1), new StreetProperty(SpotType.LB2), new StreetProperty(SpotType.LB3),
                new StreetProperty(SpotType.P1), new StreetProperty(SpotType.P2), new StreetProperty(SpotType.P3),
                new StreetProperty(SpotType.O1), new StreetProperty(SpotType.O2), new StreetProperty(SpotType.O3),
                new StreetProperty(SpotType.R1), new StreetProperty(SpotType.R2), new StreetProperty(SpotType.R3)
        );
        for (StreetProperty property : hotelProperties) {
            TestObjectFactory.giveProperty(hotelOwner, property);
            setBuildingState(property, 0, 1);
        }

        MonopolyRuntime runtime = initHeadlessRuntime();
        runtime.setGameSession(new GameSession(TestObjectFactory.playersWithTurn(owner, hotelOwner), null, null));

        assertFalse(first.buyHouses(1));
        assertEquals(4, first.getHouseCount());
        assertEquals(0, first.getHotelCount());
    }

    @Test
    void sellingHotelFailsWhenBankCannotReturnFourHouses() {
        Player owner = TestObjectFactory.player("Owner", 5000, 1);
        Player other = TestObjectFactory.player("Other", 5000, 2);
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(owner, first);
        TestObjectFactory.giveProperty(owner, second);
        setBuildingState(first, 0, 1);
        setBuildingState(second, 0, 1);

        StreetProperty filler = new StreetProperty(SpotType.LB1);
        TestObjectFactory.giveProperty(other, filler);
        setBuildingState(filler, 29, 0);

        MonopolyRuntime runtime = initHeadlessRuntime();
        runtime.setGameSession(new GameSession(TestObjectFactory.playersWithTurn(owner, other), null, null));

        assertFalse(first.sellHouses(1));
        assertEquals(0, first.getHouseCount());
        assertEquals(1, first.getHotelCount());
    }

    private void setBuildingState(StreetProperty property, int houses, int hotels) {
        setField(property, "houseCount", houses);
        setField(property, "hotelCount", hotels);
    }

    private void setField(Object target, String fieldName, int value) {
        try {
            Field field = StreetProperty.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.setInt(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
