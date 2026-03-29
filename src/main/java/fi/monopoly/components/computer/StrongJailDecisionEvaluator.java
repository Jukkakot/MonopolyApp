package fi.monopoly.components.computer;

import static fi.monopoly.text.UiTexts.text;

final class StrongJailDecisionEvaluator {
    private final StrongBotConfig config;

    StrongJailDecisionEvaluator(StrongBotConfig config) {
        this.config = config;
    }

    boolean shouldAvoidJail(GameView view, PlayerView self, PopupView popup) {
        if (popup == null || popup.message() == null || !popup.message().equals(text("jail.payOrCardPrompt"))) {
            return false;
        }
        if (self.getOutOfJailCardCount() > 0) {
            return true;
        }
        if (self.moneyAmount() < 50) {
            return false;
        }
        if (!config.preferJailLateGame()) {
            return true;
        }
        boolean earlyGame = view.unownedPropertyCount() > 10;
        boolean dangerousBoard = self.boardDangerScore() >= config.dangerCashReserve();
        boolean cashSafeAfterPayment = self.moneyAmount() - 50 >= config.minCashReserve();
        if (earlyGame && cashSafeAfterPayment) {
            return true;
        }
        return !dangerousBoard && view.unownedPropertyCount() > 5 && cashSafeAfterPayment;
    }
}
