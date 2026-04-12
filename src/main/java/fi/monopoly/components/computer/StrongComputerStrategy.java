package fi.monopoly.components.computer;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.session.SessionState;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class StrongComputerStrategy implements ComputerTurnStrategy {
    private final StrongBotConfig config = StrongBotConfig.defaults();
    private final StrongPropertyBuyEvaluator propertyBuyEvaluator = new StrongPropertyBuyEvaluator(config);
    private final StrongBuildingEvaluator buildingEvaluator = new StrongBuildingEvaluator(config);
    private final StrongUnmortgageEvaluator unmortgageEvaluator = new StrongUnmortgageEvaluator(config);
    private final StrongDebtResolver debtResolver = new StrongDebtResolver(config);
    private final StrongJailDecisionEvaluator jailDecisionEvaluator = new StrongJailDecisionEvaluator(config);
    private final SmokeTestComputerStrategy fallbackStrategy = new SmokeTestComputerStrategy();

    @Override
    public boolean takeStep(ComputerTurnContext context) {
        GameView view = context.gameView();
        PlayerView self = context.currentPlayerView();
        PopupView popup = view.popup();
        if (popup != null && popup.offeredProperty() != null) {
            ComputerDecision decision = propertyBuyEvaluator.evaluatePurchase(view, self, popup.offeredProperty());
            logDecision(self, decision);
            SessionState state = context.sessionState();
            if (state.pendingDecision() != null && state.pendingDecision().decisionType() == DecisionType.PROPERTY_PURCHASE) {
                if (decision.action() == ComputerAction.ACCEPT_POPUP) {
                    return context.submit(new BuyPropertyCommand(
                            state.sessionId(),
                            state.pendingDecision().actorPlayerId(),
                            state.pendingDecision().decisionId(),
                            popup.offeredProperty().spotType().name()
                    ));
                }
                return context.submit(new DeclinePropertyCommand(
                        state.sessionId(),
                        state.pendingDecision().actorPlayerId(),
                        state.pendingDecision().decisionId(),
                        popup.offeredProperty().spotType().name()
                ));
            }
            if (decision.action() == ComputerAction.ACCEPT_POPUP) {
                return context.acceptActivePopup();
            }
            return context.declineActivePopup();
        }
        if (popup != null && popup.message() != null && popup.message().equals(fi.monopoly.text.UiTexts.text("jail.payOrCardPrompt"))) {
            ComputerDecision decision = jailDecisionEvaluator.evaluateJailDecision(view, self, popup);
            logDecision(self, decision);
            if (decision.action() == ComputerAction.ACCEPT_POPUP) {
                return context.acceptActivePopup();
            }
            return context.declineActivePopup();
        }
        if (view.debt() != null && popup != null) {
            return context.resolveActivePopup();
        }
        if (popup != null) {
            return context.resolveActivePopup();
        }
        if (view.debt() != null) {
            return debtResolver.resolve(context, view, self);
        }
        if (view.visibleActions().endTurnVisible()) {
            BuildPlan buildPlan = buildingEvaluator.evaluateBuild(view, self);
            StrongUnmortgageEvaluator.UnmortgagePlan unmortgagePlan = unmortgageEvaluator.evaluate(view, self);
            if (buildPlan != null && (unmortgagePlan == null || buildPlan.decision().score() >= unmortgagePlan.decision().score())) {
                logDecision(self, buildPlan.decision());
                if (context.buyBuildingRound(buildPlan.target().spotType())) {
                    return true;
                }
            }
            if (unmortgagePlan != null) {
                logDecision(self, unmortgagePlan.decision());
                if (context.toggleMortgage(unmortgagePlan.target().spotType())) {
                    return true;
                }
            }
            ComputerDecision tradeDecision = context.initiateTrade();
            if (tradeDecision != null) {
                logDecision(self, tradeDecision);
                return true;
            }
        }
        if (view.visibleActions().endTurnVisible()) {
            logDecision(self, new ComputerDecision(ComputerAction.END_TURN, 0, "End turn: no stronger buy/build/debt action available"));
        }
        return fallbackStrategy.takeStep(context);
    }

    private void logDecision(PlayerView self, ComputerDecision decision) {
        log.info("Bot decision: player={}, action={}, score={}, reason={}",
                self.name(),
                decision.action(),
                round(decision.score()),
                decision.reason());
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
