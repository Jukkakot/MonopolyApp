package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.support.TestLogLevels;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameBankruptcyTest {
    private TestLogLevels.LogConfigSnapshot logConfigSnapshot;

    @BeforeEach
    void setWarnOnlyLogging() {
        logConfigSnapshot = TestLogLevels.useSimulationLogging();
    }

    @SuppressWarnings("unchecked")
    private static List<Player> getPlayers(Game game) throws ReflectiveOperationException {
        Field field = Players.class.getDeclaredField("playerList");
        field.setAccessible(true);
        return (List<Player>) field.get(game.players());
    }

    @Test
    @Timeout(5)
    void bankruptcyAgainstPlayerLiquidatesBuildingsThenTransfersAssets() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Player debtor = getPlayers(game).get(0);
        Player creditor = getPlayers(game).get(1);
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

        assertEquals(2, game.players().count());
        assertFalse(getPlayers(game).contains(debtor));
        assertEquals(1_600, creditor.getMoneyAmount());
        assertEquals(1, creditor.getGetOutOfJailCardCount());
        assertTrue(creditor.getOwnedProperties().containsAll(List.of(b1, b2)));
        assertEquals(0, b1.getBuildingLevel());
        assertEquals(0, b2.getBuildingLevel());
    }

    @Test
    @Timeout(5)
    void bankruptcyAgainstBankLiquidatesBuildingsAndAuctionsPropertiesToRemainingPlayers() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Player debtor = getPlayers(game).get(0);
        Player auctionWinner = getPlayers(game).get(1);
        Player brokePlayer = getPlayers(game).get(2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(debtor, b1);
        TestObjectFactory.giveProperty(debtor, b2);
        assertTrue(b1.buyHouses(1));
        assertTrue(b2.buyHouses(1));
        debtor.addMoney(-(debtor.getMoneyAmount() - 50));
        auctionWinner.addMoney(-(auctionWinner.getMoneyAmount() - 500));
        brokePlayer.addMoney(-(brokePlayer.getMoneyAmount() - 90));
        debtor.addOutOfJailCard();

        setDebtState(game, new DebtState(
                new PaymentRequest(debtor, BankTarget.INSTANCE, 2_000, "Bankruptcy"),
                () -> {
                },
                true
        ));

        invokeDeclareBankruptcy(game);
        settlePopupQueue(runtime);

        assertEquals(2, game.players().count());
        assertFalse(getPlayers(game).contains(debtor));
        assertTrue(auctionWinner.getOwnedProperties().stream().anyMatch(property -> property.getSpotType() == SpotType.B1));
        assertTrue(auctionWinner.getOwnedProperties().stream().anyMatch(property -> property.getSpotType() == SpotType.B2));
        assertFalse(b1.isMortgaged());
        assertFalse(b2.isMortgaged());
        assertEquals(0, b1.getBuildingLevel());
        assertEquals(0, b2.getBuildingLevel());
        assertEquals(480, auctionWinner.getMoneyAmount());
    }

    @Test
    @Timeout(5)
    void bankruptcyAgainstBankLeavesPropertiesAtBankWhenNobodyCanBid() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        Player debtor = getPlayers(game).get(0);
        Player other1 = getPlayers(game).get(1);
        Player other2 = getPlayers(game).get(2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(debtor, b1);
        TestObjectFactory.giveProperty(debtor, b2);
        debtor.addMoney(-(debtor.getMoneyAmount() - 50));
        other1.addMoney(-(other1.getMoneyAmount() - 80));
        other2.addMoney(-(other2.getMoneyAmount() - 90));

        setDebtState(game, new DebtState(
                new PaymentRequest(debtor, BankTarget.INSTANCE, 2_000, "Bankruptcy"),
                () -> {
                },
                true
        ));

        invokeDeclareBankruptcy(game);
        settlePopupQueue(runtime);

        assertEquals(2, game.players().count());
        assertFalse(getPlayers(game).contains(debtor));
        assertNull(b1.getOwnerPlayer());
        assertNull(b2.getOwnerPlayer());
        assertFalse(b1.isMortgaged());
        assertFalse(b2.isMortgaged());
    }

    @AfterEach
    void tearDown() {
        if (logConfigSnapshot != null) {
            logConfigSnapshot.restore();
        }
        DesktopClientSettings.setDebugMode(false);
        DesktopClientSettings.setSkipAnimations(false);
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

    @Test
    @Timeout(5)
    void winnerTokenStaysOnBoardAfterGameEndingBankruptcy() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime();
        Game game = new Game(runtime);

        List<Player> players = getPlayers(game);
        Player debtor = players.get(0);
        Player winner = players.get(1);
        Player third = players.get(2);
        game.players().removePlayer(third);

        setDebtState(game, new DebtState(
                new PaymentRequest(debtor, new PlayerTarget(winner), 2_000, "Bankruptcy"),
                () -> {
                },
                true
        ));

        invokeDeclareBankruptcy(game);

        assertEquals(1, game.players().count());
        assertEquals(winner, game.players().getPlayers().get(0));
        assertTrue(gameSessionState(game).gameOver());
        assertEquals(winner.getSpot().getTokenCoords(winner), winner.getCoords());
        assertTrue(runtime.popupService().isAnyVisible());
        assertNotNull(runtime.popupService().activePopupMessage());
        assertTrue(runtime.popupService().activePopupMessage().contains(winner.getName()));

        runtime.popupService().triggerPrimaryAction();

        assertFalse(runtime.popupService().isAnyVisible());
        assertFalse(game.dices().isVisible());
        assertFalse(getEndRoundButton(game).isVisible());

        invokeEndRound(game);

        assertEquals(winner, game.players().getTurn());
        assertFalse(game.dices().isVisible());
        assertFalse(getEndRoundButton(game).isVisible());
    }

    private static void setDebtState(Game game, DebtState debtState) throws ReflectiveOperationException {
        game.debtController().setDebtStateForTest(debtState);
    }

    private static GameSessionState gameSessionState(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("sessionState");
        field.setAccessible(true);
        return (GameSessionState) field.get(game);
    }

    private static MonopolyButton getEndRoundButton(Game game) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField("endRoundButton");
        field.setAccessible(true);
        return (MonopolyButton) field.get(game);
    }

    private static void invokeDeclareBankruptcy(Game game) throws ReflectiveOperationException {
        game.debtController().declareBankruptcy();
    }

    private static void invokeEndRound(Game game) throws ReflectiveOperationException {
        Method method = Game.class.getDeclaredMethod("endRound", boolean.class);
        method.setAccessible(true);
        method.invoke(game, true);
    }

    private static void settlePopupQueue(MonopolyRuntime runtime) {
        for (int i = 0; i < 20; i++) {
            runtime.eventBus().flushPendingChanges();
            if (!runtime.popupService().isAnyVisible()) {
                return;
            }
            Player turnPlayer = runtime.gameSession().players().getTurn();
            if (turnPlayer != null && turnPlayer.isComputerControlled()) {
                runtime.popupService().resolveForComputer(turnPlayer.getComputerProfile());
            } else {
                runtime.popupService().triggerPrimaryAction();
            }
        }
    }
}
