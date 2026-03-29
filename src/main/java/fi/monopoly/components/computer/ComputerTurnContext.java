package fi.monopoly.components.computer;

import fi.monopoly.types.SpotType;

public interface ComputerTurnContext {
    GameView gameView();

    PlayerView currentPlayerView();

    boolean resolveActivePopup();

    boolean sellBuilding(SpotType spotType, int count);

    boolean toggleMortgage(SpotType spotType);

    void retryPendingDebtPayment();

    void declareBankruptcy();

    void rollDice();

    void endTurn();
}
