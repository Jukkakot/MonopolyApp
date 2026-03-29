package fi.monopoly.components.computer;

import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrongComputerStrategyTest {

    private final StrongPropertyBuyEvaluator evaluator = new StrongPropertyBuyEvaluator(StrongBotConfig.defaults());
    private final StrongComputerStrategy strategy = new StrongComputerStrategy();

    @Test
    void buysPropertyThatCompletesMonopolyEvenBelowNormalReserve() {
        PlayerView self = playerView(
                1,
                270,
                List.of(propertyView(SpotType.B1, 60, 2))
        );
        PropertyView offeredProperty = propertyView(SpotType.B2, 60, 4);
        GameView gameView = gameView(self, offeredProperty, 20);

        assertTrue(evaluator.shouldBuy(gameView, self, offeredProperty));
    }

    @Test
    void declinesWeakUtilityWhenPurchaseBreaksReserve() {
        PlayerView self = playerView(1, 300, List.of());
        PropertyView offeredProperty = propertyView(SpotType.U1, 150, 28);
        GameView gameView = gameView(self, offeredProperty, 20);

        assertFalse(evaluator.shouldBuy(gameView, self, offeredProperty));
    }

    @Test
    void strongStrategyAcceptsGoodPropertyOffer() {
        PlayerView self = playerView(
                1,
                270,
                List.of(propertyView(SpotType.B1, 60, 2))
        );
        PropertyView offeredProperty = propertyView(SpotType.B2, 60, 4);
        FakeContext context = new FakeContext(gameView(self, offeredProperty, 20), self);

        assertTrue(strategy.takeStep(context));
        assertTrue(context.accepted);
        assertFalse(context.declined);
    }

    @Test
    void strongStrategyDeclinesWeakPropertyOffer() {
        PlayerView self = playerView(1, 300, List.of());
        PropertyView offeredProperty = propertyView(SpotType.U1, 150, 28);
        FakeContext context = new FakeContext(gameView(self, offeredProperty, 20), self);

        assertTrue(strategy.takeStep(context));
        assertTrue(context.declined);
        assertFalse(context.accepted);
    }

    private static GameView gameView(PlayerView self, PropertyView offeredProperty, int unownedPropertyCount) {
        return new GameView(
                self.id(),
                List.of(
                        self,
                        playerView(2, 1500, List.of(propertyView(SpotType.O1, 180, 14), propertyView(SpotType.O2, 180, 14)))
                ),
                new VisibleActionsView(true, false, false, false, false),
                new PopupView("PropertyOfferPopup", "Offer", List.of("Accept", "Decline"), offeredProperty),
                null,
                unownedPropertyCount,
                32,
                12
        );
    }

    private static PlayerView playerView(int id, int money, List<PropertyView> properties) {
        return new PlayerView(
                id,
                "P" + id,
                money,
                id,
                ComputerPlayerProfile.STRONG,
                SpotType.GO_SPOT,
                false,
                0,
                0,
                0,
                0,
                properties.stream().mapToInt(PropertyView::liquidationValue).sum(),
                280,
                List.of(),
                properties
        );
    }

    private static PropertyView propertyView(SpotType spotType, int price, int rentEstimate) {
        return new PropertyView(
                spotType,
                spotType.streetType,
                spotType.streetType.placeType,
                spotType.name(),
                price,
                false,
                price / 2,
                price / 2,
                0,
                0,
                0,
                rentEstimate,
                false
        );
    }

    private static final class FakeContext implements ComputerTurnContext {
        private final GameView gameView;
        private final PlayerView self;
        private boolean accepted;
        private boolean declined;

        private FakeContext(GameView gameView, PlayerView self) {
            this.gameView = gameView;
            this.self = self;
        }

        @Override
        public GameView gameView() {
            return gameView;
        }

        @Override
        public PlayerView currentPlayerView() {
            return self;
        }

        @Override
        public boolean resolveActivePopup() {
            accepted = true;
            return true;
        }

        @Override
        public boolean acceptActivePopup() {
            accepted = true;
            return true;
        }

        @Override
        public boolean declineActivePopup() {
            declined = true;
            return true;
        }

        @Override
        public boolean sellBuilding(SpotType spotType, int count) {
            return false;
        }

        @Override
        public boolean toggleMortgage(SpotType spotType) {
            return false;
        }

        @Override
        public void retryPendingDebtPayment() {
        }

        @Override
        public void declareBankruptcy() {
        }

        @Override
        public void rollDice() {
        }

        @Override
        public void endTurn() {
        }
    }
}
