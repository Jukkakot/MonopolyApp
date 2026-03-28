package fi.monopoly.components.computer;

import fi.monopoly.components.Player;

public interface ComputerTurnContext {
    boolean isPopupVisible();

    boolean resolvePopupForComputer(ComputerPlayerProfile profile);

    boolean isDebtResolutionActiveFor(Player player);

    int requiredDebtAmount(Player player);

    boolean isBankruptcyRiskFor(Player player);

    void retryPendingDebtPayment();

    void declareBankruptcy();

    boolean isDiceVisible();

    void rollDice();

    boolean isEndTurnVisible();

    void endTurn();
}
