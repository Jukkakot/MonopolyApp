package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBankruptcyTest {

    @Test
    void bankruptcyAgainstPlayerLiquidatesBuildingsThenTransfersAssets() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Player debtor = getPlayers().get(0);
        Player creditor = getPlayers().get(1);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(debtor, b1);
        TestObjectFactory.giveProperty(debtor, b2);
        assertTrue(b1.buyHouses(1));
        assertTrue(b2.buyHouses(1));
        debtor.addMoney(-(debtor.getMoneyAmount() - 50));
        debtor.addOutOfJailCard();

        setDebtState(game, new DebtState(
                new PaymentRequest(debtor, new PlayerTarget(creditor), 2_000, "Bankruptcy"),
                () -> {
                },
                true
        ));

        invokeDeclareBankruptcy(game);

        assertEquals(2, Game.players.count());
        assertFalse(getPlayers().contains(debtor));
        assertEquals(1_600, creditor.getMoneyAmount());
        assertEquals(1, creditor.getGetOutOfJailCardCount());
        assertTrue(creditor.getOwnedProperties().containsAll(List.of(b1, b2)));
        assertEquals(0, b1.getBuildingLevel());
        assertEquals(0, b2.getBuildingLevel());
    }

    @Test
    void bankruptcyAgainstBankLiquidatesBuildingsAndReturnsPropertiesToBank() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Player debtor = getPlayers().get(0);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(debtor, b1);
        TestObjectFactory.giveProperty(debtor, b2);
        assertTrue(b1.buyHouses(1));
        assertTrue(b2.buyHouses(1));
        debtor.addMoney(-(debtor.getMoneyAmount() - 50));
        debtor.addOutOfJailCard();

        setDebtState(game, new DebtState(
                new PaymentRequest(debtor, BankTarget.INSTANCE, 2_000, "Bankruptcy"),
                () -> {
                },
                true
        ));

        invokeDeclareBankruptcy(game);

        assertEquals(2, Game.players.count());
        assertFalse(getPlayers().contains(debtor));
        assertNull(b1.getOwnerPlayer());
        assertNull(b2.getOwnerPlayer());
        assertFalse(b1.isMortgaged());
        assertFalse(b2.isMortgaged());
        assertEquals(0, b1.getBuildingLevel());
        assertEquals(0, b2.getBuildingLevel());
    }

    @AfterEach
    void tearDown() {
        MonopolyApp.DEBUG_MODE = false;
        MonopolyApp.SKIP_ANNIMATIONS = false;
        fi.monopoly.components.spots.JailSpot.jailTimeLeftMap.clear();
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

    private static void resetNextPlayerId() throws ReflectiveOperationException {
        Field field = Player.class.getDeclaredField("NEXT_ID");
        field.setAccessible(true);
        field.setInt(null, 0);
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayers() throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField("playerList");
        field.setAccessible(true);
        return (List<Player>) field.get(Game.players);
    }

    private static void setDebtState(Game game, DebtState debtState) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("debtState");
        field.setAccessible(true);
        field.set(game, debtState);
    }

    private static void invokeDeclareBankruptcy(Game game) throws ReflectiveOperationException {
        Method method = Game.class.getDeclaredMethod("declareBankruptcy");
        method.setAccessible(true);
        method.invoke(game);
    }
}
