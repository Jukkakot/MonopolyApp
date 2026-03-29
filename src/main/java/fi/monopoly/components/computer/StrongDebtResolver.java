package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

import java.util.Comparator;

final class StrongDebtResolver {
    boolean resolve(ComputerTurnContext context, GameView view, PlayerView self) {
        int amount = view.debt().amount();
        if (self.moneyAmount() < amount) {
            liquidateAssets(context, amount);
        }
        if (context.currentPlayerView().moneyAmount() >= amount) {
            context.retryPendingDebtPayment();
            return true;
        }
        if (view.debt().bankruptcyRisk()) {
            context.declareBankruptcy();
            return true;
        }
        return false;
    }

    private void liquidateAssets(ComputerTurnContext context, int targetAmount) {
        while (context.currentPlayerView().moneyAmount() < targetAmount) {
            PlayerView current = context.currentPlayerView();
            PropertyView buildingSale = selectBuildingSale(current);
            if (buildingSale != null && context.sellBuilding(buildingSale.spotType(), 1)) {
                continue;
            }
            PropertyView mortgage = selectMortgage(current);
            if (mortgage != null && context.toggleMortgage(mortgage.spotType())) {
                continue;
            }
            return;
        }
    }

    private PropertyView selectBuildingSale(PlayerView player) {
        return player.ownedProperties().stream()
                .filter(property -> property.placeType() == PlaceType.STREET)
                .filter(property -> property.buildingLevel() > 0)
                .min(Comparator
                        .comparingInt(this::buildingSalePriority)
                        .thenComparingInt(PropertyView::rentEstimate)
                        .thenComparingInt(PropertyView::price))
                .orElse(null);
    }

    private PropertyView selectMortgage(PlayerView player) {
        return player.ownedProperties().stream()
                .filter(property -> !property.mortgaged())
                .filter(this::canMortgage)
                .min(Comparator
                        .comparingInt(this::mortgagePriority)
                        .thenComparingInt(PropertyView::rentEstimate)
                        .thenComparingInt(PropertyView::mortgageValue))
                .orElse(null);
    }

    private int buildingSalePriority(PropertyView property) {
        int score = 0;
        if (property.completedSet()) {
            score += 1000;
        }
        score += property.buildingLevel() >= 3 ? 200 : 50;
        score += streetStrength(property.streetType()) * 10;
        return score;
    }

    private int mortgagePriority(PropertyView property) {
        int score = 0;
        if (property.completedSet()) {
            score += 1000;
        }
        score += ownsCompetingPieces(property) ? 400 : 0;
        score += streetStrength(property.streetType()) * 10;
        if (property.placeType() == PlaceType.UTILITY) {
            score -= 20;
        }
        if (property.placeType() == PlaceType.RAILROAD) {
            score -= 10;
        }
        return score;
    }

    private boolean ownsCompetingPieces(PropertyView property) {
        return switch (property.placeType()) {
            case STREET -> !property.completedSet();
            case RAILROAD, UTILITY -> false;
            default -> false;
        };
    }

    private boolean canMortgage(PropertyView property) {
        return property.placeType() != PlaceType.STREET || property.buildingLevel() == 0;
    }

    private int streetStrength(StreetType streetType) {
        return switch (streetType) {
            case ORANGE, RED, YELLOW -> 5;
            case LIGHT_BLUE, PURPLE, GREEN -> 4;
            case DARK_BLUE, RAILROAD -> 3;
            case BROWN, UTILITY -> 2;
            default -> 1;
        };
    }
}
