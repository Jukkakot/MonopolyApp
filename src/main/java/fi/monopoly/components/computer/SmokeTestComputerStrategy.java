package fi.monopoly.components.computer;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.PlaceType;

import java.util.Comparator;

final class SmokeTestComputerStrategy implements ComputerTurnStrategy {
    @Override
    public boolean takeStep(ComputerTurnContext context, Player player) {
        if (context.isPopupVisible()) {
            return context.resolvePopupForComputer(player.getComputerProfile());
        }
        if (context.isDebtResolutionActiveFor(player)) {
            return resolveDebt(context, player);
        }
        if (context.isDiceVisible()) {
            context.rollDice();
            return true;
        }
        if (context.isEndTurnVisible()) {
            context.endTurn();
            return true;
        }
        return false;
    }

    private boolean resolveDebt(ComputerTurnContext context, Player debtor) {
        int amount = context.requiredDebtAmount(debtor);
        if (debtor.getMoneyAmount() < amount) {
            liquidateAssets(debtor, amount);
        }
        if (debtor.getMoneyAmount() >= amount) {
            context.retryPendingDebtPayment();
            return true;
        }
        if (context.isBankruptcyRiskFor(debtor)) {
            context.declareBankruptcy();
            return true;
        }
        return false;
    }

    private void liquidateAssets(Player player, int targetAmount) {
        while (player.getMoneyAmount() < targetAmount && sellOneBuilding(player)) {
            // Keep selling until the player has enough cash or cannot liquidate more buildings.
        }
        if (player.getMoneyAmount() >= targetAmount) {
            return;
        }
        for (Property property : player.getOwnedProperties().stream()
                .filter(property -> !property.isMortgaged())
                .filter(this::canMortgage)
                .sorted(Comparator.comparingInt(Property::getMortgageValue))
                .toList()) {
            property.handleMortgaging();
            if (player.getMoneyAmount() >= targetAmount) {
                return;
            }
        }
    }

    private boolean sellOneBuilding(Player player) {
        for (StreetProperty property : player.getOwnedProperties().stream()
                .filter(StreetProperty.class::isInstance)
                .map(StreetProperty.class::cast)
                .filter(property -> property.getMaxSellableHouseCount() > 0)
                .sorted(Comparator.comparingInt(StreetProperty::getHousePrice).reversed())
                .toList()) {
            if (property.sellHouses(1)) {
                return true;
            }
        }
        return false;
    }

    private boolean canMortgage(Property property) {
        if (property.getSpotType().streetType.placeType != PlaceType.STREET) {
            return true;
        }
        return !(property instanceof StreetProperty streetProperty) || !streetProperty.hasBuildings();
    }
}
