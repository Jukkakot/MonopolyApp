package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;

import java.util.Comparator;
import java.util.List;

final class StrongBuildingEvaluator {
    private final StrongBotConfig config;

    StrongBuildingEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    PropertyView selectBuildTarget(GameView view, PlayerView self) {
        BuildPlan plan = evaluateBuild(view, self);
        return plan == null ? null : plan.target();
    }

    BuildPlan evaluateBuild(GameView view, PlayerView self) {
        PropertyView target = candidateStreetSets(self).stream()
                .filter(property -> canAffordRound(view, self, property))
                .max(Comparator.<PropertyView>comparingDouble(property -> buildScore(view, self, property))
                        .thenComparingInt(property -> streetStrength(property.streetType()))
                        .thenComparingInt(PropertyView::housePrice))
                .orElse(null);
        if (target == null) {
            return null;
        }
        double score = buildScore(view, self, target);
        int roundCost = roundCost(self, target.streetType());
        return new BuildPlan(
                target,
                new ComputerDecision(
                        ComputerAction.BUILD_ROUND,
                        score,
                        "Build " + target.streetType().name() + " round: score " + round(score) + ", cost M" + roundCost + ", reserve after build M" + (self.moneyAmount() - roundCost)
                )
        );
    }

    private List<PropertyView> candidateStreetSets(PlayerView self) {
        return self.ownedProperties().stream()
                .filter(property -> property.placeType() == PlaceType.STREET)
                .filter(PropertyView::completedSet)
                .filter(property -> !property.mortgaged())
                .filter(property -> property.buildingLevel() < 5)
                .filter(property -> self.ownedProperties().stream()
                        .filter(other -> other.streetType() == property.streetType())
                        .noneMatch(PropertyView::mortgaged))
                .sorted(Comparator.comparing(PropertyView::spotType))
                .toList();
    }

    private boolean canAffordRound(GameView view, PlayerView self, PropertyView property) {
        int roundCost = roundCost(self, property.streetType());
        return roundCost > 0 && self.moneyAmount() - roundCost >= requiredReserve(view, self);
    }

    private double buildScore(GameView view, PlayerView self, PropertyView property) {
        int roundCost = roundCost(self, property.streetType());
        if (roundCost <= 0) {
            return Double.NEGATIVE_INFINITY;
        }
        double score = streetStrength(property.streetType()) * 3.0;
        score += expectedRentGrowth(self, property.streetType()) / (double) roundCost * 100.0;
        score += cheapHouseBonus(self, property.streetType());
        if (config.prioritizeThreeHouses() && averageLevel(self, property.streetType()) < 3.0) {
            score += 4.0;
        }
        if (view.unownedPropertyCount() > 8) {
            score -= 2.0;
        }
        if (self.boardDangerScore() >= config.dangerCashReserve()) {
            score -= 6.0;
        }
        return score;
    }

    private int requiredReserve(GameView view, PlayerView self) {
        return self.boardDangerScore() >= config.dangerCashReserve() || view.unownedPropertyCount() <= 10
                ? config.dangerCashReserve()
                : config.minCashReserve();
    }

    private int roundCost(PlayerView self, StreetType streetType) {
        return self.ownedProperties().stream()
                .filter(property -> property.streetType() == streetType)
                .mapToInt(PropertyView::housePrice)
                .sum();
    }

    private double averageLevel(PlayerView self, StreetType streetType) {
        return self.ownedProperties().stream()
                .filter(property -> property.streetType() == streetType)
                .mapToInt(PropertyView::buildingLevel)
                .average()
                .orElse(0);
    }

    private int expectedRentGrowth(PlayerView self, StreetType streetType) {
        return self.ownedProperties().stream()
                .filter(property -> property.streetType() == streetType)
                .mapToInt(property -> projectedRent(property) - property.rentEstimate())
                .sum();
    }

    private int projectedRent(PropertyView property) {
        SpotType spotType = property.spotType();
        List<Integer> rents = List.of(spotType.getStringProperty("rents").split(",")).stream()
                .map(Integer::parseInt)
                .toList();
        int nextLevel = Math.min(5, property.buildingLevel() + 1);
        return rents.get(nextLevel);
    }

    private double cheapHouseBonus(PlayerView self, StreetType streetType) {
        return 100.0 / Math.max(1, roundCost(self, streetType));
    }

    private int streetStrength(StreetType streetType) {
        return switch (streetType) {
            case ORANGE, RED -> 5;
            case YELLOW, LIGHT_BLUE -> 4;
            case GREEN, PURPLE -> 3;
            case DARK_BLUE, BROWN -> 2;
            default -> 1;
        };
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
