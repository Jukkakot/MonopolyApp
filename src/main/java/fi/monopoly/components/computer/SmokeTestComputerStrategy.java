package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;

import java.util.Comparator;

final class SmokeTestComputerStrategy implements ComputerTurnStrategy {
    @Override
    public boolean takeStep(ComputerTurnContext context) {
        GameView view = context.gameView();
        PlayerView player = context.currentPlayerView();
        if (view.visibleActions().popupVisible()) {
            return context.resolveActivePopup();
        }
        if (view.debt() != null) {
            return resolveDebt(context, player);
        }
        if (view.visibleActions().rollDiceVisible()) {
            context.rollDice();
            return true;
        }
        if (view.visibleActions().endTurnVisible()) {
            context.endTurn();
            return true;
        }
        return false;
    }

    private boolean resolveDebt(ComputerTurnContext context, PlayerView debtor) {
        int amount = context.gameView().debt().amount();
        if (debtor.moneyAmount() < amount) {
            liquidateAssets(context, debtor, amount);
        }
        if (context.currentPlayerView().moneyAmount() >= amount) {
            context.retryPendingDebtPayment();
            return true;
        }
        if (context.gameView().debt().bankruptcyRisk()) {
            context.declareBankruptcy();
            return true;
        }
        return false;
    }

    private void liquidateAssets(ComputerTurnContext context, PlayerView player, int targetAmount) {
        while (context.currentPlayerView().moneyAmount() < targetAmount && sellOneBuilding(context, player)) {
            // Keep selling until the player has enough cash or cannot liquidate more buildings.
        }
        if (context.currentPlayerView().moneyAmount() >= targetAmount) {
            return;
        }
        for (PropertyView property : player.ownedProperties().stream()
                .filter(property -> !property.mortgaged())
                .filter(this::canMortgage)
                .sorted(Comparator.comparingInt(PropertyView::mortgageValue))
                .toList()) {
            context.toggleMortgage(property.spotType());
            if (context.currentPlayerView().moneyAmount() >= targetAmount) {
                return;
            }
        }
    }

    private boolean sellOneBuilding(ComputerTurnContext context, PlayerView player) {
        for (PropertyView property : player.ownedProperties().stream()
                .filter(property -> property.placeType() == PlaceType.STREET)
                .filter(property -> property.buildingLevel() > 0)
                .sorted(Comparator.comparingInt(PropertyView::price).reversed())
                .toList()) {
            if (context.sellBuilding(property.spotType(), 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean canMortgage(PropertyView property) {
        if (property.placeType() != PlaceType.STREET) {
            return true;
        }
        return property.buildingLevel() == 0;
    }
}
