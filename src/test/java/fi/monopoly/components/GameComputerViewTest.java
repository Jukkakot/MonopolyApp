package fi.monopoly.components;

import controlP5.ControlP5;
import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.lang.reflect.Field;
import java.util.Objects;

import static fi.monopoly.text.UiTexts.text;
import static org.junit.jupiter.api.Assertions.*;

class GameComputerViewTest {

    private static MonopolyRuntime initHeadlessRuntime(int width, int height) {
        MonopolyApp app = new MonopolyApp();
        app.width = width;
        app.height = height;

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

    @AfterEach
    void tearDown() {
        DesktopClientSettings.setDebugMode(false);
        DesktopClientSettings.setSkipAnimations(false);
        JailSpot.jailTimeLeftMap.clear();
    }

    @Test
    void computerViewExposesReadOnlySnapshotForCurrentPlayer() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        Player turnPlayer = game.testFacade().players().getTurn();

        turnPlayer.addOwnedProperty(PropertyFactory.getProperty(SpotType.DB1));
        turnPlayer.addOwnedProperty(PropertyFactory.getProperty(SpotType.B2));
        JailSpot.jailTimeLeftMap.put(turnPlayer, 2);

        GameView view = game.testFacade().createGameView(turnPlayer);
        PlayerView self = view.currentPlayer().orElseThrow();

        assertEquals(turnPlayer.getId(), view.currentPlayerId());
        assertTrue(view.unownedPropertyCount() >= 0);
        assertEquals(turnPlayer.getName(), self.name());
        assertEquals(2, self.jailRoundsLeft());
        assertEquals(turnPlayer.getTotalLiquidationValue(), self.totalLiquidationValue());
        assertFalse(self.ownedProperties().isEmpty());
        assertNotNull(self.ownedProperties().get(0).spotType());
        assertNotNull(self.ownedProperties().get(0).name());
    }

    @Test
    void computerViewIncludesPopupAndDebtSnapshots() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);
        Player turnPlayer = game.testFacade().players().getTurn();

        runtime.popupService().show("Test popup", () -> {
        }, () -> {
        });
        turnPlayer.releaseAssetsToBank();
        turnPlayer.addMoney(40 - turnPlayer.getMoneyAmount());
        game.testFacade().handlePaymentRequest(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, 250, text("game.debug.reason.railDebt")),
                null,
                (CallbackAction) () -> {
                }
        );

        GameView view = game.testFacade().createGameView(turnPlayer);

        assertNotNull(view.popup());
        assertEquals("ChoicePopup", view.popup().type());
        assertEquals(2, view.popup().actions().size());
        assertNotNull(view.debt());
        assertEquals(250, view.debt().amount());
        assertTrue(view.visibleActions().popupVisible());
        assertTrue(view.visibleActions().retryDebtVisible());
        assertTrue(view.visibleActions().declareBankruptcyVisible());
    }

    @Test
    void defaultSeatsUseStrongBotProfile() throws ReflectiveOperationException {
        resetNextPlayerId();
        MonopolyRuntime runtime = initHeadlessRuntime(MonopolyApp.DEFAULT_WINDOW_WIDTH, MonopolyApp.DEFAULT_WINDOW_HEIGHT);
        Game game = new Game(runtime);

        GameView view = game.testFacade().createGameView(game.testFacade().players().getTurn());

        assertEquals(3, view.players().size());
        assertTrue(view.players().stream().map(PlayerView::computerProfile).allMatch(Objects::nonNull));
        assertTrue(view.players().stream().anyMatch(player -> player.computerProfile() == ComputerPlayerProfile.STRONG));
    }
}
