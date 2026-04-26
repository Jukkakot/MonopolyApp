package fi.monopoly.components.computer;

import fi.monopoly.application.command.DeclareBankruptcyCommand;
import fi.monopoly.application.command.MortgagePropertyForDebtCommand;
import fi.monopoly.application.command.PayDebtCommand;
import fi.monopoly.application.command.SellBuildingForDebtCommand;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
@RequiredArgsConstructor
final class StrongDebtResolver {
    private final StrongBotConfig config;

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
            return context.submit(new PayDebtCommand(
                    context.sessionState().sessionId(),
                    playerId(context.currentPlayerView()),
                    context.sessionState().activeDebt().debtId()
            ));
        }
        if (view.debt().bankruptcyRisk()) {
            logDebtDecision(context.currentPlayerView(), new ComputerDecision(
                    ComputerAction.DECLARE_BANKRUPTCY,
                    -1000,
                    "Declare bankruptcy: no liquidation path covers debt M" + amount
            ));
            return context.submit(new DeclareBankruptcyCommand(
                    context.sessionState().sessionId(),
                    playerId(context.currentPlayerView()),
                    context.sessionState().activeDebt().debtId()
            ));
        }
        return false;
    }

    private void liquidateAssets(ComputerTurnContext context, int targetAmount) {
        while (context.currentPlayerView().moneyAmount() < targetAmount) {
            PlayerView current = context.currentPlayerView();
            DebtStep buildingSale = selectBuildingSale(current);
            if (buildingSale != null && context.submit(new SellBuildingForDebtCommand(
                    context.sessionState().sessionId(),
                    playerId(current),
                    context.sessionState().activeDebt().debtId(),
                    buildingSale.property().spotType().name(),
                    1
            ))) {
                logDebtDecision(current, buildingSale.decision());
                continue;
            }
            DebtStep mortgage = selectMortgage(current);
            if (mortgage != null && context.submit(new MortgagePropertyForDebtCommand(
                    context.sessionState().sessionId(),
                    playerId(current),
                    context.sessionState().activeDebt().debtId(),
                    mortgage.property().spotType().name()
            ))) {
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
            score += scaledPremiumPenalty(1000);
        }
        score += property.buildingLevel() >= 3 ? scaledPremiumPenalty(200) : scaledPremiumPenalty(50);
        score += scaledPremiumPenalty(streetStrength(property.streetType()) * 10);
        return score;
    }

    private int mortgagePriority(PropertyView property) {
        int score = 0;
        if (property.completedSet()) {
            score += scaledPremiumPenalty(1000);
        }
        score += ownsCompetingPieces(property) ? scaledPremiumPenalty(400) : 0;
        score += scaledPremiumPenalty(streetStrength(property.streetType()) * 10);
        if (property.placeType() == PlaceType.UTILITY) {
            score -= 20;
        }
        if (property.placeType() == PlaceType.RAILROAD) {
            score -= 10;
        }
        return score;
    }

    private int scaledPremiumPenalty(int basePenalty) {
        return Math.max(10, (int) Math.round(basePenalty / Math.max(0.1, config.bankruptcyAversion())));
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

    private String playerId(PlayerView player) {
        return "player-" + player.id();
    }

    private record DebtStep(
            PropertyView property,
            ComputerDecision decision
    ) {
    }
}
