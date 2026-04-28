package fi.monopoly.components.computer;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclareBankruptcyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.command.EndTurnCommand;
import fi.monopoly.application.command.MortgagePropertyForDebtCommand;
import fi.monopoly.application.command.PayDebtCommand;
import fi.monopoly.application.command.RollDiceCommand;
import fi.monopoly.application.command.SellBuildingForDebtCommand;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.PlaceType;

import java.util.Comparator;

final class SmokeTestComputerStrategy implements ComputerTurnStrategy {
    @Override
    public boolean takeStep(ComputerTurnContext context) {
        GameView view = context.gameView();
        PlayerView player = context.currentPlayerView();
        SessionState state = context.sessionState();
        if (view.popup() != null && view.popup().offeredProperty() != null
                && state.pendingDecision() != null
                && state.pendingDecision().decisionType() == DecisionType.PROPERTY_PURCHASE) {
            if (player.moneyAmount() >= view.popup().offeredProperty().price()) {
                return context.submit(new BuyPropertyCommand(
                        state.sessionId(),
                        state.pendingDecision().actorPlayerId(),
                        state.pendingDecision().decisionId(),
                        view.popup().offeredProperty().spotType().name()
                ));
            }
            return context.submit(new DeclinePropertyCommand(
                    state.sessionId(),
                    state.pendingDecision().actorPlayerId(),
                    state.pendingDecision().decisionId(),
                    view.popup().offeredProperty().spotType().name()
            ));
        }
        if (view.visibleActions().popupVisible()) {
            return context.resolveActivePopup();
        }
        if (view.debt() != null) {
            return resolveDebt(context, player);
        }
        if (view.visibleActions().rollDiceVisible()) {
            return context.submit(new RollDiceCommand(state.sessionId(), playerId(player)));
        }
        if (view.visibleActions().endTurnVisible()) {
            return context.submit(new EndTurnCommand(state.sessionId(), playerId(player)));
        }
        return false;
    }

    private boolean resolveDebt(ComputerTurnContext context, PlayerView debtor) {
        int amount = context.gameView().debt().amount();
        if (debtor.moneyAmount() < amount) {
            liquidateAssets(context, debtor, amount);
        }
        SessionState state = context.sessionState();
        if (context.currentPlayerView().moneyAmount() >= amount) {
            return context.submit(new PayDebtCommand(state.sessionId(), playerId(context.currentPlayerView()), state.activeDebt().debtId()));
        }
        if (context.gameView().debt().bankruptcyRisk()) {
            return context.submit(new DeclareBankruptcyCommand(state.sessionId(), playerId(context.currentPlayerView()), state.activeDebt().debtId()));
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
            if (context.sessionState().activeDebt() == null) {
                return;
            }
            context.submit(new MortgagePropertyForDebtCommand(
                    context.sessionState().sessionId(),
                    playerId(context.currentPlayerView()),
                    context.sessionState().activeDebt().debtId(),
                    property.spotType().name()
            ));
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
            if (context.sessionState().activeDebt() != null && context.submit(new SellBuildingForDebtCommand(
                    context.sessionState().sessionId(),
                    playerId(context.currentPlayerView()),
                    context.sessionState().activeDebt().debtId(),
                    property.spotType().name(),
                    1
            ))) {
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

    private String playerId(PlayerView player) {
        return "player-" + player.id();
    }
}
