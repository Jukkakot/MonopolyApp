package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;

final class StrongPropertyBuyEvaluator {
    private final StrongBotConfig config;

    StrongPropertyBuyEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    boolean shouldBuy(GameView gameView, PlayerView self, PropertyView property) {
        return evaluatePurchase(gameView, self, property).action() == ComputerAction.ACCEPT_POPUP;
    }

    ComputerDecision evaluatePurchase(GameView gameView, PlayerView self, PropertyView property) {
        if (property == null) {
            return new ComputerDecision(ComputerAction.DECLINE_POPUP, Double.NEGATIVE_INFINITY, "No property offer to evaluate");
        }
        int reserve = requiredReserve(gameView, self);
        int postPurchaseCash = self.moneyAmount() - property.price();
        if (postPurchaseCash < 0) {
            return new ComputerDecision(ComputerAction.DECLINE_POPUP, -1000, "Decline " + property.name() + ": cannot afford price M" + property.price());
        }
        boolean completesSet = wouldCompleteSet(self, property);
        double score = score(gameView, self, property);
        if (completesSet) {
            boolean accept = postPurchaseCash >= Math.max(0, reserve - 100);
            return new ComputerDecision(
                    accept ? ComputerAction.ACCEPT_POPUP : ComputerAction.DECLINE_POPUP,
                    score + 10,
                    (accept ? "Buy " : "Decline ") + property.name() + ": completes set, post-cash M" + postPurchaseCash + ", reserve M" + reserve
            );
        }
        if (postPurchaseCash < reserve) {
            return new ComputerDecision(
                    ComputerAction.DECLINE_POPUP,
                    score,
                    "Decline " + property.name() + ": post-cash M" + postPurchaseCash + " falls below reserve M" + reserve
            );
        }
        double threshold = buyThreshold(gameView, property);
        boolean accept = score >= threshold;
        return new ComputerDecision(
                accept ? ComputerAction.ACCEPT_POPUP : ComputerAction.DECLINE_POPUP,
                score,
                (accept ? "Buy " : "Decline ") + property.name() + ": score " + round(score) + " vs threshold " + round(threshold)
        );
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

    private double buyThreshold(GameView gameView, PropertyView property) {
        if (gameView.unownedPropertyCount() > 20) {
            return switch (property.placeType()) {
                case STREET -> 1.5;
                case RAILROAD -> 2.75;
                case UTILITY -> 2.0;
                default -> config.buyThreshold();
            };
        }
        if (gameView.unownedPropertyCount() > 10) {
            return switch (property.placeType()) {
                case STREET -> 2.5;
                case RAILROAD -> 3.5;
                case UTILITY -> 4.5;
                default -> config.buyThreshold();
            };
        }
        return config.buyThreshold();
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

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
