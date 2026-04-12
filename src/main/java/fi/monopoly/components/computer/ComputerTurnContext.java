package fi.monopoly.components.computer;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.SpotType;

public interface ComputerTurnContext {
    GameView gameView();

    PlayerView currentPlayerView();

    SessionState sessionState();

    boolean submit(SessionCommand command);

    boolean resolveActivePopup();

    boolean acceptActivePopup();

    boolean declineActivePopup();

    boolean sellBuilding(SpotType spotType, int count);

    boolean buyBuildingRound(SpotType spotType);

    boolean toggleMortgage(SpotType spotType);

    ComputerDecision initiateTrade();

    void retryPendingDebtPayment();

    void declareBankruptcy();

    void rollDice();

    void endTurn();
}
