package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyTurnEffectExecutorTest {

    @Test
    void executeCallsOnCompleteImmediatelyWhenNoEffectsExist() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();

        executor.execute(List.of(), callback);

        assertEquals(1, callback.callCount);
        assertTrue(popupService.messages.isEmpty());
    }

    @Test
    void showMessageEffectDisplaysMessageAndCompletes() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();

        executor.execute(List.of(new ShowMessageEffect("hello")), callback);

        assertEquals(List.of("hello"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void adjustPlayerMoneyEffectUpdatesMoneyAfterPopupAccept() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Owner", 1000, 1);

        executor.execute(List.of(new AdjustPlayerMoneyEffect(player, 50, "gain 50")), callback);

        assertEquals(1050, player.getMoneyAmounnt());
        assertEquals(List.of("gain 50"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectBuysPropertyWhenAccepted() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 1500, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), callback);

        assertEquals(player, property.getOwnerPlayer());
        assertEquals(1440, player.getMoneyAmounnt());
        assertEquals(List.of("buy it"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectShowsFailureMessageWhenPlayerCannotAffordProperty() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 10, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), callback);

        assertEquals(null, property.getOwnerPlayer());
        assertEquals(10, player.getMoneyAmounnt());
        assertEquals(List.of("buy it", "You don't have enough money to buy MEDITER- RANEAN AVENUE"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void payRentEffectTransfersMoneyAfterPopupAccept() {
        TestPopupService popupService = new TestPopupService();
        LegacyTurnEffectExecutor executor = new LegacyTurnEffectExecutor(popupService);
        TestCallbackAction callback = new TestCallbackAction();
        Player from = TestObjectFactory.player("From", 1000, 1);
        Player to = TestObjectFactory.player("To", 1000, 2);

        executor.execute(List.of(new PayRentEffect(from, to, 200, "pay rent")), callback);

        assertEquals(800, from.getMoneyAmounnt());
        assertEquals(1200, to.getMoneyAmounnt());
        assertEquals(List.of("pay rent"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    private static final class TestPopupService extends PopupService {
        private final List<String> messages = new ArrayList<>();
        private final ArrayDeque<Boolean> choiceResponses = new ArrayDeque<>();

        private TestPopupService() {
            super(null);
        }

        @Override
        public void show(String text) {
            messages.add(text);
        }

        @Override
        public void show(String text, ButtonAction onAccept) {
            messages.add(text);
            if (onAccept != null) {
                onAccept.doAction();
            }
        }

        @Override
        public void show(String text, ButtonAction onAccept, ButtonAction onDecline) {
            messages.add(text);
            boolean accept = choiceResponses.isEmpty() || choiceResponses.removeFirst();
            if (accept) {
                if (onAccept != null) {
                    onAccept.doAction();
                }
            } else if (onDecline != null) {
                onDecline.doAction();
            }
        }
    }

    private static final class TestCallbackAction implements CallbackAction {
        private int callCount = 0;

        @Override
        public void doAction() {
            callCount++;
        }
    }
}
