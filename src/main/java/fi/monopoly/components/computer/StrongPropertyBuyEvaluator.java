package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

final class StrongPropertyBuyEvaluator {
    private final StrongBotConfig config;

    StrongPropertyBuyEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    boolean shouldBuy(GameView gameView, PlayerView self, PropertyView property) {
        if (property == null) {
            return false;
        }
        int reserve = requiredReserve(gameView, self);
        int postPurchaseCash = self.moneyAmount() - property.price();
        if (postPurchaseCash < 0) {
            return false;
        }
        boolean completesSet = wouldCompleteSet(self, property);
        if (completesSet) {
            return postPurchaseCash >= Math.max(0, reserve - 100);
        }
        if (postPurchaseCash < reserve) {
            return false;
        }
        return score(gameView, self, property) >= config.buyThreshold();
    }

    double score(GameView gameView, PlayerView self, PropertyView property) {
        double score = 0;
        score += config.completionWeight() * completionValue(self, property);
        score += config.progressWeight() * progressValue(self, property);
        score += config.opponentBlockWeight() * blockValue(gameView, self, property);
        score += typeValue(property);
        score -= config.liquidityPenaltyWeight() * liquidityRisk(gameView, self, property);
        return score;
    }

    private int requiredReserve(GameView gameView, PlayerView self) {
        return self.boardDangerScore() >= config.dangerCashReserve() || gameView.unownedPropertyCount() <= 10
                ? config.dangerCashReserve()
                : config.minCashReserve();
    }

    private double completionValue(PlayerView self, PropertyView property) {
        return wouldCompleteSet(self, property) ? 1.0 : 0.0;
    }

    private double progressValue(PlayerView self, PropertyView property) {
        int ownedInSet = ownedInSet(self, property.streetType());
        int setSize = setSize(property.streetType());
        if (setSize <= 1) {
            return 0;
        }
        return (double) (ownedInSet + 1) / setSize;
    }

    private double blockValue(GameView gameView, PlayerView self, PropertyView property) {
        if (!config.buyToBlockOpponent()) {
            return 0;
        }
        int setSize = setSize(property.streetType());
        if (setSize <= 1) {
            return 0;
        }
        int bestOpponentCount = gameView.players().stream()
                .filter(player -> player.id() != self.id())
                .mapToInt(player -> ownedInSet(player, property.streetType()))
                .max()
                .orElse(0);
        return bestOpponentCount == setSize - 1 ? 1.0 : 0.0;
    }

    private double typeValue(PropertyView property) {
        return switch (property.placeType()) {
            case RAILROAD -> config.railroadWeight();
            case UTILITY -> config.utilityWeight();
            case STREET -> property.rentEstimate() >= 20 ? 1.5 : 0.5;
            default -> 0;
        };
    }

    private double liquidityRisk(GameView gameView, PlayerView self, PropertyView property) {
        int reserve = requiredReserve(gameView, self);
        int postPurchaseCash = self.moneyAmount() - property.price();
        return Math.max(0, reserve - postPurchaseCash) / 100.0;
    }

    private boolean wouldCompleteSet(PlayerView self, PropertyView property) {
        return ownedInSet(self, property.streetType()) + 1 >= setSize(property.streetType());
    }

    private int ownedInSet(PlayerView player, StreetType streetType) {
        return (int) player.ownedProperties().stream()
                .filter(ownedProperty -> ownedProperty.streetType() == streetType)
                .count();
    }

    private int setSize(StreetType streetType) {
        return switch (streetType.placeType) {
            case STREET -> streetType == StreetType.BROWN || streetType == StreetType.DARK_BLUE ? 2 : 3;
            case RAILROAD -> 4;
            case UTILITY -> 2;
            default -> 0;
        };
    }
}
