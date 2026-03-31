package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
final class StrongDebtResolver {
    boolean resolve(ComputerTurnContext context, GameView view, PlayerView self) {
        int amount = view.debt().amount();
        if (self.moneyAmount() < amount) {
            liquidateAssets(context, amount);
        }
        if (context.currentPlayerView().moneyAmount() >= amount) {
            logDebtDecision(context.currentPlayerView(), new ComputerDecision(
                    ComputerAction.RETRY_DEBT_PAYMENT,
                    0,
                    "Retry debt payment after raising cash to M" + context.currentPlayerView().moneyAmount()
            ));
            context.retryPendingDebtPayment();
            return true;
        }
        if (view.debt().bankruptcyRisk()) {
            logDebtDecision(context.currentPlayerView(), new ComputerDecision(
                    ComputerAction.DECLARE_BANKRUPTCY,
                    -1000,
                    "Declare bankruptcy: no liquidation path covers debt M" + amount
            ));
            context.declareBankruptcy();
            return true;
        }
        return false;
    }

    private void liquidateAssets(ComputerTurnContext context, int targetAmount) {
        while (context.currentPlayerView().moneyAmount() < targetAmount) {
            PlayerView current = context.currentPlayerView();
            DebtStep buildingSale = selectBuildingSale(current);
            if (buildingSale != null && context.sellBuilding(buildingSale.property().spotType(), 1)) {
                logDebtDecision(current, buildingSale.decision());
                continue;
            }
            DebtStep mortgage = selectMortgage(current);
            if (mortgage != null && context.toggleMortgage(mortgage.property().spotType())) {
                logDebtDecision(current, mortgage.decision());
                continue;
            }
            return;
        }
    }

    private DebtStep selectBuildingSale(PlayerView player) {
        PropertyView property = player.ownedProperties().stream()
                .filter(candidate -> candidate.placeType() == PlaceType.STREET)
                .filter(candidate -> candidate.buildingLevel() > 0)
                .filter(candidate -> canSellBuilding(player, candidate))
                .min(Comparator
                        .comparingInt(this::buildingSalePriority)
                        .thenComparingInt(PropertyView::rentEstimate)
                        .thenComparingInt(PropertyView::price))
                .orElse(null);
        if (property == null) {
            return null;
        }
        int priority = buildingSalePriority(property);
        return new DebtStep(
                property,
                new ComputerDecision(
                        ComputerAction.SELL_BUILDING,
                        -priority,
                        "Sell building on " + property.name() + ": low priority score " + priority + " among build groups"
                )
        );
    }

    private DebtStep selectMortgage(PlayerView player) {
        PropertyView property = player.ownedProperties().stream()
                .filter(candidate -> !candidate.mortgaged())
                .filter(candidate -> canMortgage(player, candidate))
                .min(Comparator
                        .comparingInt(this::mortgagePriority)
                        .thenComparingInt(PropertyView::rentEstimate)
                        .thenComparingInt(PropertyView::mortgageValue))
                .orElse(null);
        if (property == null) {
            return null;
        }
        int priority = mortgagePriority(property);
        return new DebtStep(
                property,
                new ComputerDecision(
                        ComputerAction.MORTGAGE_PROPERTY,
                        -priority,
                        "Mortgage " + property.name() + ": liquidation priority " + priority
                )
        );
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

    private boolean canSellBuilding(PlayerView player, PropertyView property) {
        if (property.placeType() != PlaceType.STREET || property.buildingLevel() <= 0) {
            return false;
        }
        int maxGroupLevel = player.ownedProperties().stream()
                .filter(candidate -> candidate.streetType() == property.streetType())
                .mapToInt(PropertyView::buildingLevel)
                .max()
                .orElse(property.buildingLevel());
        return property.buildingLevel() == maxGroupLevel;
    }

    private boolean canMortgage(PlayerView player, PropertyView property) {
        if (property.placeType() != PlaceType.STREET) {
            return true;
        }
        return player.ownedProperties().stream()
                .filter(candidate -> candidate.streetType() == property.streetType())
                .noneMatch(candidate -> candidate.buildingLevel() > 0);
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

    private void logDebtDecision(PlayerView player, ComputerDecision decision) {
        log.info("Bot decision: player={}, action={}, score={}, reason={}",
                player.name(),
                decision.action(),
                round(decision.score()),
                decision.reason());
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record DebtStep(
            PropertyView property,
            ComputerDecision decision
    ) {
    }
}
