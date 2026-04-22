package fi.monopoly.components.spots;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.*;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.cards.Card;
import fi.monopoly.components.cards.Cards;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.images.SpotImage;
import fi.monopoly.support.TestDesktopRuntimeFactory;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.CardType;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.Coordinates;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import processing.event.KeyEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static fi.monopoly.text.UiTexts.setLocale;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static processing.event.KeyEvent.PRESS;

class PickCardSpotTest {

    private MonopolyRuntime runtime;

    @BeforeEach
    void setUp() {
        setLocale(Locale.ENGLISH);
        runtime = initHeadlessRuntime();
        runtime.setGameSession(new GameSession(null, null, new Animations()));
    }

    @AfterEach
    void tearDown() {
        setLocale(Locale.ENGLISH);
    }

    @Test
    void chairmanCardPaysEachOtherPlayerThroughPaymentHandler() {
        Player turnPlayer = TestObjectFactory.player("Turn", 1500, 1);
        turnPlayer.addMoney(500);
        Player otherOne = TestObjectFactory.player("One", 1500, 2);
        Player otherTwo = TestObjectFactory.player("Two", 1500, 3);
        Players players = TestObjectFactory.playersWithTurn(turnPlayer, otherOne, otherTwo);
        PickCardSpot spot = pickCardSpotWith(new Card(CardType.ALL_PLAYERS_MONEY, "Chairman", List.of("-50")));
        RecordingPaymentHandler paymentHandler = new RecordingPaymentHandler();
        TestCallback callback = new TestCallback();

        spot.handleTurn(new GameState(players, null, null, null, paymentHandler), callback);
        settlePendingPopups();

        assertEquals(2, paymentHandler.requests.size());
        assertEquals(turnPlayer, paymentHandler.requests.get(0).debtor());
        assertEquals(otherOne, ((PlayerTarget) paymentHandler.requests.get(0).target()).player());
        assertEquals(turnPlayer, paymentHandler.requests.get(1).debtor());
        assertEquals(otherTwo, ((PlayerTarget) paymentHandler.requests.get(1).target()).player());
        assertEquals(1900, turnPlayer.getMoneyAmount());
        assertEquals(1550, otherOne.getMoneyAmount());
        assertEquals(1550, otherTwo.getMoneyAmount());
        assertEquals(1, callback.callCount);
    }

    @Test
    void birthdayCardCollectsFromEachOtherPlayerThroughPaymentHandler() {
        Player turnPlayer = TestObjectFactory.player("Turn", 1500, 1);
        Player otherOne = TestObjectFactory.player("One", 1500, 2);
        Player otherTwo = TestObjectFactory.player("Two", 1500, 3);
        Players players = TestObjectFactory.playersWithTurn(turnPlayer, otherOne, otherTwo);
        PickCardSpot spot = pickCardSpotWith(new Card(CardType.ALL_PLAYERS_MONEY, "Birthday", List.of("10")));
        RecordingPaymentHandler paymentHandler = new RecordingPaymentHandler();
        TestCallback callback = new TestCallback();

        spot.handleTurn(new GameState(players, null, null, null, paymentHandler), callback);
        settlePendingPopups();

        assertEquals(2, paymentHandler.requests.size());
        assertEquals(otherOne, paymentHandler.requests.get(0).debtor());
        assertEquals(turnPlayer, ((PlayerTarget) paymentHandler.requests.get(0).target()).player());
        assertEquals(otherTwo, paymentHandler.requests.get(1).debtor());
        assertEquals(turnPlayer, ((PlayerTarget) paymentHandler.requests.get(1).target()).player());
        assertEquals(1520, turnPlayer.getMoneyAmount());
        assertEquals(1490, otherOne.getMoneyAmount());
        assertEquals(1490, otherTwo.getMoneyAmount());
        assertEquals(1, callback.callCount);
    }

    @Test
    void communityChestConsultancyFeeMatchesCardTextAmount() {
        String moneyCards = ResourceBundle.getBundle("community", Locale.ENGLISH).getString("MONEY");

        org.junit.jupiter.api.Assertions.assertTrue(moneyCards.contains("Receive M25 consultancy fee;25"));
    }

    private PickCardSpot pickCardSpotWith(Card card) {
        try {
            PickCardSpot spot = new PickCardSpot(new SpotImage(runtime, new Coordinates(100, 100), SpotType.COMMUNITY1));
            Field cardsField = PickCardSpot.class.getDeclaredField("cards");
            cardsField.setAccessible(true);
            cardsField.set(spot, new FixedCards(card));
            return spot;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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

    private static MonopolyRuntime initHeadlessRuntime() {
        return TestDesktopRuntimeFactory.create().runtime();
    }

    private static final class FixedCards extends Cards {
        private final Card card;

        private FixedCards(Card card) {
            super(fi.monopoly.types.StreetType.COMMUNITY);
            this.card = card;
        }

        @Override
        public Card getCard() {
            return card;
        }
    }

    private static final class RecordingPaymentHandler implements PaymentHandler {
        private final List<PaymentRequest> requests = new ArrayList<>();

        @Override
        public void requestPayment(PaymentRequest request, CallbackAction onResolved) {
            requests.add(request);
            request.debtor().addMoney(-request.amount());
            if (request.target() instanceof PlayerTarget playerTarget) {
                playerTarget.player().addMoney(request.amount());
            }
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
