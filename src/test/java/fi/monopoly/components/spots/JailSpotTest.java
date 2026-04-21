package fi.monopoly.components.spots;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.*;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.images.SpotImage;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.text.UiTexts;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import fi.monopoly.utils.Coordinates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;
import processing.event.KeyEvent;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static processing.event.KeyEvent.PRESS;

class JailSpotTest {

    private MonopolyRuntime runtime;
    private JailSpot jailSpot;

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = 1700;
        app.height = 996;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }

    @BeforeEach
    void setUp() {
        UiTexts.setLocale(Locale.ENGLISH);
        runtime = initHeadlessRuntime();
        runtime.setGameSession(new GameSession(null, null, new Animations()));
        jailSpot = new JailSpot(new SpotImage(runtime, new Coordinates(100, 100), SpotType.JAIL));
        JailSpot.jailTimeLeftMap.clear();
    }

    @AfterEach
    void tearDown() {
        UiTexts.setLocale(Locale.ENGLISH);
        JailSpot.jailTimeLeftMap.clear();
    }

    @Test
    void handleTurnRequestsPaymentWhenPlayerChoosesToAvoidJailWithCash() {
        Player player = new Player(runtime, "Turn", javafx.scene.paint.Color.BLACK, jailSpot);
        Players players = TestObjectFactory.playersWithTurn(player);
        TestPaymentHandler paymentHandler = new TestPaymentHandler();
        TestCallback callback = new TestCallback();

        jailSpot.handleTurn(
                new GameState(players, null, null, TurnResult.builder().shouldGoToJail(true).build(), paymentHandler),
                callback
        );
        settlePendingPopups();

        assertNotNull(paymentHandler.request);
        assertEquals(50, paymentHandler.request.amount());
        assertEquals("Pay M50 to avoid jail", paymentHandler.request.reason());
        assertFalse(player.isInJail());
        assertEquals(1, callback.callCount);
    }

    @Test
    void handleInJailTurnRequestsPaymentOnFinalRoundWithoutDoubles() {
        Player player = new Player(runtime, "Turn", javafx.scene.paint.Color.BLACK, jailSpot);
        JailSpot.jailTimeLeftMap.put(player, 1);
        TestPaymentHandler paymentHandler = new TestPaymentHandler();
        TestCallback gotOut = new TestCallback();
        TestCallback stayed = new TestCallback();

        jailSpot.handleInJailTurn(player, new DiceValue(DiceState.NOREROLL, 5), paymentHandler, gotOut, stayed);
        settlePendingPopups();

        assertNotNull(paymentHandler.request);
        assertEquals(50, paymentHandler.request.amount());
        assertEquals("Pay M50 to get out of jail", paymentHandler.request.reason());
        assertFalse(player.isInJail());
        assertEquals(1, gotOut.callCount);
        assertEquals(0, stayed.callCount);
    }

    @Test
    void handleInJailTurnReducesRoundsWhenNotFinalRoundAndNoDoubles() {
        Player player = new Player(runtime, "Turn", javafx.scene.paint.Color.BLACK, jailSpot);
        JailSpot.jailTimeLeftMap.put(player, 3);
        TestPaymentHandler paymentHandler = new TestPaymentHandler();
        TestCallback gotOut = new TestCallback();
        TestCallback stayed = new TestCallback();

        jailSpot.handleInJailTurn(player, new DiceValue(DiceState.NOREROLL, 5), paymentHandler, gotOut, stayed);
        settlePendingPopups();

        assertTrue(player.isInJail());
        assertEquals(2, JailSpot.jailTimeLeftMap.get(player));
        assertEquals(0, gotOut.callCount);
        assertEquals(1, stayed.callCount);
        assertNull(paymentHandler.request);
    }

    private void settlePendingPopups() {
        for (int i = 0; i < 10 && runtime.popupService().isAnyVisible(); i++) {
            dispatchKey('1');
        }
    }

    private void dispatchKey(char key) {
        runtime.eventBus().sendConsumableEvent(
                new KeyEvent(new Object(), System.currentTimeMillis(), PRESS, 0, key, key)
        );
        runtime.eventBus().flushPendingChanges();
        runtime.popupService().showNextPending();
    }

    private static final class TestPaymentHandler implements PaymentHandler {
        private PaymentRequest request;

        @Override
        public void requestPayment(PaymentRequest request, CallbackAction onResolved) {
            this.request = request;
            request.debtor().addMoney(-request.amount());
            onResolved.doAction();
        }
    }

    private static final class TestCallback implements CallbackAction {
        private int callCount;

        @Override
        public void doAction() {
            callCount++;
        }
    }
}
