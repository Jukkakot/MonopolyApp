package fi.monopoly.components.computer;

import static fi.monopoly.text.UiTexts.text;

final class StrongJailDecisionEvaluator {
    private final StrongBotConfig config;

    StrongJailDecisionEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    boolean shouldAvoidJail(GameView view, PlayerView self, PopupView popup) {
        return evaluateJailDecision(view, self, popup).action() == ComputerAction.ACCEPT_POPUP;
    }

    ComputerDecision evaluateJailDecision(GameView view, PlayerView self, PopupView popup) {
        if (popup == null || popup.message() == null || !popup.message().equals(text("jail.payOrCardPrompt"))) {
            return new ComputerDecision(ComputerAction.DECLINE_POPUP, Double.NEGATIVE_INFINITY, "No jail prompt to evaluate");
        }
        if (self.getOutOfJailCardCount() > 0) {
            return new ComputerDecision(ComputerAction.ACCEPT_POPUP, 10, "Avoid jail: use get out of jail card");
        }
        if (self.moneyAmount() < 50) {
            return new ComputerDecision(ComputerAction.DECLINE_POPUP, -10, "Stay in jail: cannot afford M50");
        }
        if (!config.preferJailLateGame()) {
            return new ComputerDecision(ComputerAction.ACCEPT_POPUP, 5, "Avoid jail: late-game jail preference disabled");
        }
        boolean earlyGame = view.unownedPropertyCount() > 10;
        boolean dangerousBoard = self.boardDangerScore() >= config.jailExitThreshold();
        boolean cashSafeAfterPayment = self.moneyAmount() - 50 >= StrongReservePolicy.requiredReserve(config, view, self);
        if (earlyGame && cashSafeAfterPayment) {
            return new ComputerDecision(ComputerAction.ACCEPT_POPUP, 8, "Avoid jail: early game with safe post-cash M" + (self.moneyAmount() - 50));
        }
        boolean accept = !dangerousBoard && view.unownedPropertyCount() > 5 && cashSafeAfterPayment;
        return new ComputerDecision(
                accept ? ComputerAction.ACCEPT_POPUP : ComputerAction.DECLINE_POPUP,
                accept ? 4 : -4,
                accept
                        ? "Avoid jail: mid game and board danger manageable"
                        : "Stay in jail: dangerous board " + self.boardDangerScore() + " with only " + view.unownedPropertyCount() + " unowned properties left"
        );
    }
}
