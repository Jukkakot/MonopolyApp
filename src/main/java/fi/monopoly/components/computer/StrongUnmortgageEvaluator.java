package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

import java.util.Comparator;

final class StrongUnmortgageEvaluator {
    private final StrongBotConfig config;

    StrongUnmortgageEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    UnmortgagePlan evaluate(GameView view, PlayerView self) {
        PropertyView target = self.ownedProperties().stream()
                .filter(PropertyView::mortgaged)
                .filter(property -> canAfford(view, self, property))
                .max(Comparator.<PropertyView>comparingDouble(property -> score(view, self, property))
                        .thenComparingInt(property -> streetStrength(property.streetType()))
                        .thenComparingInt(PropertyView::rentEstimate))
                .orElse(null);
        if (target == null) {
            return null;
        }
        int cost = unmortgageCost(target);
        return new UnmortgagePlan(
                target,
                new ComputerDecision(
                        ComputerAction.UNMORTGAGE_PROPERTY,
                        score(view, self, target),
                        "Unmortgage " + target.name() + ": cost M" + cost + ", post-cash M" + (self.moneyAmount() - cost)
                )
        );
    }

    private boolean canAfford(GameView view, PlayerView self, PropertyView property) {
        return self.moneyAmount() - unmortgageCost(property) >= requiredReserve(view, self);
    }

    private int requiredReserve(GameView view, PlayerView self) {
        return StrongReservePolicy.requiredReserve(config, view, self);
    }

    private double score(GameView view, PlayerView self, PropertyView property) {
        double score = streetStrength(property.streetType()) * 2.5;
        score += property.rentEstimate() / 20.0;
        if (property.completedSet()) {
            score += 12.0 + config.developmentBias();
        }
        if (property.placeType() == PlaceType.RAILROAD) {
            score += 2.0;
        }
        if (property.placeType() == PlaceType.UTILITY) {
            score -= 1.0;
        }
        if (view.unownedPropertyCount() > 10) {
            score -= 2.0;
        }
        score *= config.unmortgageAggression();
        score *= config.mortgageRecoveryPriority();
        score *= config.colorGroupWeight(property.streetType());
        return score;
    }

    private int unmortgageCost(PropertyView property) {
        return property.mortgageValue() + property.mortgageValue() / 10;
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

    record UnmortgagePlan(
            PropertyView target,
            ComputerDecision decision
    ) {
    }
}
