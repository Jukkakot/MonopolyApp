package fi.monopoly.components.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.text.UiTexts;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class InteractiveTurnEffectExecutorTest {
    private final PaymentHandler paymentHandler = (request, onResolved) -> {
        request.debtor().addMoney(-request.amount());
        if (!(request.target() instanceof BankTarget)) {
            ((fi.monopoly.components.payment.PlayerTarget) request.target()).player().addMoney(request.amount());
        }
        onResolved.doAction();
    };

    @BeforeEach
    void setEnglishLocale() {
        UiTexts.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void resetEnglishLocale() {
        UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void executeCallsOnCompleteImmediatelyWhenNoEffectsExist() {
        TestPopupService popupService = new TestPopupService();
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, new Players(null));
        TestCallbackAction callback = new TestCallbackAction();

        executor.execute(List.of(), paymentHandler, callback);

        assertEquals(1, callback.callCount);
        assertTrue(popupService.messages.isEmpty());
    }

    @Test
    void showMessageEffectDisplaysMessageAndCompletes() {
        TestPopupService popupService = new TestPopupService();
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, new Players(null));
        TestCallbackAction callback = new TestCallbackAction();

        executor.execute(List.of(new ShowMessageEffect("hello")), paymentHandler, callback);

        assertEquals(List.of("hello"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void adjustPlayerMoneyEffectUpdatesMoneyAfterPopupAccept() {
        TestPopupService popupService = new TestPopupService();
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, new Players(null));
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Owner", 1000, 1);

        executor.execute(List.of(new AdjustPlayerMoneyEffect(player, 50, "gain 50")), paymentHandler, callback);

        assertEquals(1050, player.getMoneyAmount());
        assertEquals(List.of("gain 50"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectBuysPropertyWhenAccepted() {
        TestPopupService popupService = new TestPopupService();
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 1500, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, TestObjectFactory.playersWithTurn(player));

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertEquals(player, property.getOwnerPlayer());
        assertEquals(1440, player.getMoneyAmount());
        assertEquals(List.of("buy it"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectShowsFailureMessageWhenPlayerCannotAffordProperty() {
        TestPopupService popupService = new TestPopupService();
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 10, 1);
        Player auctionWinner = new Player("AuctionWinner", Color.BLACK, 500, 2, ComputerPlayerProfile.SMOKE_TEST);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, TestObjectFactory.playersWithTurn(player, auctionWinner));

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertEquals(auctionWinner, property.getOwnerPlayer());
        assertEquals(10, player.getMoneyAmount());
        assertEquals(480, auctionWinner.getMoneyAmount());
        assertEquals(List.of(
                "buy it",
                "You don't have enough money to buy MEDITER- RANEAN AVENUE",
                "AuctionWinner won the auction for MEDITER- RANEAN AVENUE with M20"
        ), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectStartsAuctionWhenPlayerDeclines() {
        TestPopupService popupService = new TestPopupService();
        popupService.choiceResponses.add(false);
        popupService.customButtonResponses.add(1);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 1500, 1);
        Player smokeBidder = new Player("SmokeBidder", Color.BLACK, 500, 2, ComputerPlayerProfile.SMOKE_TEST);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, TestObjectFactory.playersWithTurn(player, smokeBidder));

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertEquals(smokeBidder, property.getOwnerPlayer());
        assertEquals(480, smokeBidder.getMoneyAmount());
        assertEquals(List.of(
                "buy it",
                "Buyer, auction for MEDITER- RANEAN AVENUE\nBuyer is auctioning the property.",
                "SmokeBidder won the auction for MEDITER- RANEAN AVENUE with M20"
        ), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectLetsHumanPassAuctionWithoutForcedPurchase() {
        TestPopupService popupService = new TestPopupService();
        popupService.choiceResponses.add(false);
        popupService.customButtonResponses.add(1);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 1500, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, TestObjectFactory.playersWithTurn(player));

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertNull(property.getOwnerPlayer());
        assertEquals(1500, player.getMoneyAmount());
        assertEquals(List.of(
                "buy it",
                "Buyer, auction for MEDITER- RANEAN AVENUE\nBuyer is auctioning the property.",
                "No one bid on MEDITER- RANEAN AVENUE. It remains with the bank."
        ), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void humanPassLeavesAuctionPermanentlyEvenIfBotsKeepBidding() {
        TestPopupService popupService = new TestPopupService();
        popupService.choiceResponses.add(false);
        popupService.customButtonResponses.add(1);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 1500, 1);
        Player smokeBidderOne = new Player("SmokeOne", Color.BLACK, 500, 2, ComputerPlayerProfile.SMOKE_TEST);
        Player smokeBidderTwo = new Player("SmokeTwo", Color.BLUE, 500, 3, ComputerPlayerProfile.SMOKE_TEST);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(
                popupService,
                TestObjectFactory.playersWithTurn(player, smokeBidderOne, smokeBidderTwo)
        );

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertEquals(smokeBidderOne, property.getOwnerPlayer());
        assertEquals(1, popupService.messages.stream()
                .filter(message -> message.startsWith("Buyer, auction for"))
                .count(), "Human should be prompted only once after passing");
        assertEquals(440, smokeBidderOne.getMoneyAmount());
        assertEquals(500, smokeBidderTwo.getMoneyAmount());
        assertEquals(1500, player.getMoneyAmount());
        assertEquals(1, callback.callCount);
    }

    @Test
    void offerToBuyPropertyEffectLeavesPropertyAtBankWhenNobodyBids() {
        TestPopupService popupService = new TestPopupService();
        popupService.choiceResponses.add(false);
        TestCallbackAction callback = new TestCallbackAction();
        Player player = TestObjectFactory.player("Buyer", 5, 1);
        Player brokeBidder = new Player("BrokeBidder", Color.BLACK, 80, 2, ComputerPlayerProfile.SMOKE_TEST);
        StreetProperty property = new StreetProperty(SpotType.B1);
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, TestObjectFactory.playersWithTurn(player, brokeBidder));

        executor.execute(List.of(new OfferToBuyPropertyEffect(player, property, "buy it")), paymentHandler, callback);

        assertNull(property.getOwnerPlayer());
        assertEquals(5, player.getMoneyAmount());
        assertEquals(80, brokeBidder.getMoneyAmount());
        assertEquals(List.of(
                "buy it",
                "No one bid on MEDITER- RANEAN AVENUE. It remains with the bank."
        ), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    @Test
    void payRentEffectTransfersMoneyAfterPopupAccept() {
        TestPopupService popupService = new TestPopupService();
        InteractiveTurnEffectExecutor executor = new InteractiveTurnEffectExecutor(popupService, new Players(null));
        TestCallbackAction callback = new TestCallbackAction();
        Player from = TestObjectFactory.player("From", 1000, 1);
        Player to = TestObjectFactory.player("To", 1000, 2);

        executor.execute(List.of(new PayRentEffect(from, to, 200, "pay rent")), paymentHandler, callback);

        assertEquals(800, from.getMoneyAmount());
        assertEquals(1200, to.getMoneyAmount());
        assertEquals(List.of("pay rent"), popupService.messages);
        assertEquals(1, callback.callCount);
    }

    private static final class TestPopupService extends PopupService {
        private final List<String> messages = new ArrayList<>();
        private final ArrayDeque<Boolean> choiceResponses = new ArrayDeque<>();
        private final ArrayDeque<Integer> customButtonResponses = new ArrayDeque<>();

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

        @Override
        public void show(String text, ButtonProps... buttonProps) {
            messages.add(text);
            if (buttonProps.length == 0) {
                return;
            }
            int responseIndex = customButtonResponses.isEmpty() ? 0 : customButtonResponses.removeFirst();
            int clampedIndex = Math.max(0, Math.min(responseIndex, buttonProps.length - 1));
            if (buttonProps[clampedIndex].buttonAction() != null) {
                buttonProps[clampedIndex].buttonAction().doAction();
            }
        }

        @Override
        public void showManualDecision(String text, ButtonProps... buttonProps) {
            show(text, buttonProps);
        }

        @Override
        public void showPropertyAuction(Property property, String title, String reasonText, Player currentLeader, int currentBidAmount, String primaryLabel, String secondaryLabel, ButtonAction onAccept, ButtonAction onDecline) {
            messages.add(title + "\n" + reasonText);
            int responseIndex = customButtonResponses.isEmpty() ? 0 : customButtonResponses.removeFirst();
            if (responseIndex <= 0) {
                if (onAccept != null) {
                    onAccept.doAction();
                }
                return;
            }
            if (onDecline != null) {
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
