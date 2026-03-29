package fi.monopoly.components.computer;

import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void strongStrategySellsWeakBuildingsBeforeTouchingMonopolyGroup() {
        PropertyView monopoly = propertyView(SpotType.O1, 180, 80, 3, true);
        PropertyView weakBuild = propertyView(SpotType.B1, 60, 10, 1, false);
        PlayerView self = playerView(1, 0, List.of(monopoly, weakBuild));
        FakeContext context = new FakeContext(debtGameView(self, 10), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("sell:B1", "retry"), context.operations);
        assertFalse(context.bankrupt);
    }

    @Test
    void strongStrategyMortgagesIsolatedPropertyBeforeIncompleteSet() {
        PropertyView isolatedRailroad = propertyView(SpotType.RR1, 200, 25, 0, false);
        PropertyView promisingStreet = propertyView(SpotType.O1, 180, 14, 0, false);
        PlayerView self = playerView(1, 0, List.of(promisingStreet, isolatedRailroad));
        FakeContext context = new FakeContext(debtGameView(self, 100), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("mortgage:RR1", "retry"), context.operations);
    }

    @Test
    void strongStrategyDeclaresBankruptcyWhenNoLiquidationPathExists() {
        PlayerView self = playerView(1, 0, List.of());
        FakeContext context = new FakeContext(debtGameView(self, 100), self);

        assertTrue(strategy.takeStep(context));
        assertTrue(context.bankrupt);
        assertEquals(List.of("bankrupt"), context.operations);
    }

    @Test
    void strongStrategyBuildsBestMonopolyBeforeEndingTurn() {
        PlayerView self = playerView(1, 900, List.of(
                propertyView(SpotType.O1, 180, 14, 0, true, 100),
                propertyView(SpotType.O2, 180, 14, 0, true, 100),
                propertyView(SpotType.O3, 200, 16, 0, true, 100),
                propertyView(SpotType.B1, 60, 4, 0, true, 50),
                propertyView(SpotType.B2, 60, 4, 0, true, 50)
        ));
        FakeContext context = new FakeContext(endTurnGameView(self), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("build:O1"), context.operations);
    }

    @Test
    void strongStrategyKeepsReserveAndEndsTurnWithoutBuilding() {
        PlayerView self = playerView(1, 320, List.of(
                propertyView(SpotType.O1, 180, 14, 0, true, 100),
                propertyView(SpotType.O2, 180, 14, 0, true, 100),
                propertyView(SpotType.O3, 200, 16, 0, true, 100)
        ));
        FakeContext context = new FakeContext(endTurnGameView(self), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("endTurn"), context.operations);
    }

    @Test
    void strongStrategyAvoidsJailEarlyWhenCashIsSafe() {
        PlayerView self = playerView(1, 600, List.of());
        FakeContext context = new FakeContext(jailPopupGameView(self, 18), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("accept"), context.operations);
    }

    @Test
    void strongStrategyStaysInJailLateWhenBoardIsDangerous() {
        PlayerView self = playerView(1, 600, 450, List.of());
        FakeContext context = new FakeContext(jailPopupGameView(self, 4), self);

        assertTrue(strategy.takeStep(context));
        assertEquals(List.of("decline"), context.operations);
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

    private static GameView endTurnGameView(PlayerView self) {
        return new GameView(
                self.id(),
                List.of(self),
                new VisibleActionsView(false, false, false, false, true),
                null,
                null,
                4,
                32,
                12
        );
    }

    private static GameView jailPopupGameView(PlayerView self, int unownedPropertyCount) {
        return new GameView(
                self.id(),
                List.of(self),
                new VisibleActionsView(true, false, false, false, false),
                new PopupView("ChoicePopup", text("jail.payOrCardPrompt"), List.of("Accept", "Decline"), null),
                null,
                unownedPropertyCount,
                32,
                12
        );
    }

    private static GameView debtGameView(PlayerView self, int debtAmount) {
        return new GameView(
                self.id(),
                List.of(self),
                new VisibleActionsView(false, true, true, false, false),
                null,
                new DebtView(debtAmount, "Debt", true, "Bank", "Bank"),
                8,
                32,
                12
        );
    }

    private static PlayerView playerView(int id, int money, List<PropertyView> properties) {
        return playerView(id, money, 280, properties);
    }

    private static PlayerView playerView(int id, int money, int boardDangerScore, List<PropertyView> properties) {
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
                boardDangerScore,
                List.of(),
                properties
        );
    }

    private static PropertyView propertyView(SpotType spotType, int price, int rentEstimate) {
        return propertyView(spotType, price, rentEstimate, 0, false);
    }

    private static PropertyView propertyView(SpotType spotType, int price, int rentEstimate, int buildingLevel, boolean completedSet) {
        int housePrice = spotType.streetType.placeType == fi.monopoly.types.PlaceType.STREET
                ? spotType.getIntegerProperty("housePrice")
                : 0;
        return propertyView(spotType, price, rentEstimate, buildingLevel, completedSet, housePrice);
    }

    private static PropertyView propertyView(SpotType spotType, int price, int rentEstimate, int buildingLevel, boolean completedSet, int housePrice) {
        return new PropertyView(
                spotType,
                spotType.streetType,
                spotType.streetType.placeType,
                spotType.name(),
                price,
                false,
                price / 2,
                price / 2 + buildingLevel * (price / 4),
                housePrice,
                buildingLevel,
                buildingLevel == 5 ? 0 : buildingLevel,
                buildingLevel == 5 ? 1 : 0,
                rentEstimate,
                completedSet
        );
    }

    private static final class FakeContext implements ComputerTurnContext {
        private final List<PlayerView> players;
        private final VisibleActionsView visibleActions;
        private final PopupView popup;
        private final DebtView debt;
        private final int unownedPropertyCount;
        private final int bankHousesLeft;
        private final int bankHotelsLeft;
        private PlayerView self;
        private boolean accepted;
        private boolean declined;
        private boolean bankrupt;
        private final List<String> operations = new ArrayList<>();

        private FakeContext(GameView gameView, PlayerView self) {
            this.players = new ArrayList<>(gameView.players());
            this.visibleActions = gameView.visibleActions();
            this.popup = gameView.popup();
            this.debt = gameView.debt();
            this.unownedPropertyCount = gameView.unownedPropertyCount();
            this.bankHousesLeft = gameView.bankHousesLeft();
            this.bankHotelsLeft = gameView.bankHotelsLeft();
            this.self = self;
        }

        @Override
        public GameView gameView() {
            return new GameView(
                    self.id(),
                    List.copyOf(players),
                    visibleActions,
                    popup,
                    debt,
                    unownedPropertyCount,
                    bankHousesLeft,
                    bankHotelsLeft
            );
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
            operations.add("accept");
            return true;
        }

        @Override
        public boolean declineActivePopup() {
            declined = true;
            operations.add("decline");
            return true;
        }

        @Override
        public boolean sellBuilding(SpotType spotType, int count) {
            PropertyView property = findProperty(spotType);
            if (property == null || property.buildingLevel() < count) {
                return false;
            }
            int houseCount = Math.max(0, property.houseCount() - count);
            int newBuildingLevel = Math.max(0, property.buildingLevel() - count);
            replaceProperty(new PropertyView(
                    property.spotType(),
                    property.streetType(),
                    property.placeType(),
                    property.name(),
                    property.price(),
                    property.mortgaged(),
                    property.mortgageValue(),
                    property.liquidationValue(),
                    property.housePrice(),
                    newBuildingLevel,
                    houseCount,
                    property.hotelCount(),
                    property.rentEstimate(),
                    property.completedSet()
            ));
            self = new PlayerView(
                    self.id(),
                    self.name(),
                    self.moneyAmount() + count * Math.max(1, property.price() / 4),
                    self.turnNumber(),
                    self.computerProfile(),
                    self.currentSpot(),
                    self.inJail(),
                    self.jailRoundsLeft(),
                    self.getOutOfJailCardCount(),
                    self.totalHouseCount(),
                    self.totalHotelCount(),
                    self.totalLiquidationValue(),
                    self.boardDangerScore(),
                    self.completedSets(),
                    self.ownedProperties()
            );
            operations.add("sell:" + spotType.name());
            replacePlayer();
            return true;
        }

        @Override
        public boolean buyBuildingRound(SpotType spotType) {
            PropertyView property = findProperty(spotType);
            if (property == null) {
                return false;
            }
            List<PropertyView> updated = self.ownedProperties().stream()
                    .map(owned -> owned.streetType() == property.streetType()
                            ? new PropertyView(
                            owned.spotType(),
                            owned.streetType(),
                            owned.placeType(),
                            owned.name(),
                            owned.price(),
                            owned.mortgaged(),
                            owned.mortgageValue(),
                            owned.liquidationValue(),
                            owned.housePrice(),
                            owned.buildingLevel() + 1,
                            owned.houseCount() + 1,
                            owned.hotelCount(),
                            owned.rentEstimate() + 20,
                            owned.completedSet()
                    )
                            : owned)
                    .toList();
            int roundCost = updated.stream()
                    .filter(owned -> owned.streetType() == property.streetType())
                    .mapToInt(PropertyView::housePrice)
                    .sum();
            self = new PlayerView(
                    self.id(),
                    self.name(),
                    self.moneyAmount() - roundCost,
                    self.turnNumber(),
                    self.computerProfile(),
                    self.currentSpot(),
                    self.inJail(),
                    self.jailRoundsLeft(),
                    self.getOutOfJailCardCount(),
                    self.totalHouseCount(),
                    self.totalHotelCount(),
                    self.totalLiquidationValue(),
                    self.boardDangerScore(),
                    self.completedSets(),
                    updated
            );
            operations.add("build:" + spotType.name());
            replacePlayer();
            return true;
        }

        @Override
        public boolean toggleMortgage(SpotType spotType) {
            PropertyView property = findProperty(spotType);
            if (property == null || property.mortgaged()) {
                return false;
            }
            replaceProperty(new PropertyView(
                    property.spotType(),
                    property.streetType(),
                    property.placeType(),
                    property.name(),
                    property.price(),
                    true,
                    property.mortgageValue(),
                    property.liquidationValue(),
                    property.housePrice(),
                    property.buildingLevel(),
                    property.houseCount(),
                    property.hotelCount(),
                    property.rentEstimate(),
                    property.completedSet()
            ));
            self = new PlayerView(
                    self.id(),
                    self.name(),
                    self.moneyAmount() + property.mortgageValue(),
                    self.turnNumber(),
                    self.computerProfile(),
                    self.currentSpot(),
                    self.inJail(),
                    self.jailRoundsLeft(),
                    self.getOutOfJailCardCount(),
                    self.totalHouseCount(),
                    self.totalHotelCount(),
                    self.totalLiquidationValue(),
                    self.boardDangerScore(),
                    self.completedSets(),
                    self.ownedProperties()
            );
            operations.add("mortgage:" + spotType.name());
            replacePlayer();
            return true;
        }

        @Override
        public void retryPendingDebtPayment() {
            operations.add("retry");
        }

        @Override
        public void declareBankruptcy() {
            bankrupt = true;
            operations.add("bankrupt");
        }

        @Override
        public void rollDice() {
        }

        @Override
        public void endTurn() {
            operations.add("endTurn");
        }

        private PropertyView findProperty(SpotType spotType) {
            return self.ownedProperties().stream()
                    .filter(property -> property.spotType() == spotType)
                    .findFirst()
                    .orElse(null);
        }

        private void replaceProperty(PropertyView replacement) {
            List<PropertyView> properties = self.ownedProperties().stream()
                    .map(property -> property.spotType() == replacement.spotType() ? replacement : property)
                    .toList();
            self = new PlayerView(
                    self.id(),
                    self.name(),
                    self.moneyAmount(),
                    self.turnNumber(),
                    self.computerProfile(),
                    self.currentSpot(),
                    self.inJail(),
                    self.jailRoundsLeft(),
                    self.getOutOfJailCardCount(),
                    self.totalHouseCount(),
                    self.totalHotelCount(),
                    self.totalLiquidationValue(),
                    self.boardDangerScore(),
                    self.completedSets(),
                    properties
            );
        }

        private void replacePlayer() {
            players.clear();
            players.add(self);
        }
    }
}
