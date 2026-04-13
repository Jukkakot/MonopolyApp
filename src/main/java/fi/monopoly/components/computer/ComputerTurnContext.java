package fi.monopoly.components.computer;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.domain.session.SessionState;
public interface ComputerTurnContext {
    GameView gameView();

    PlayerView currentPlayerView();

    SessionState sessionState();

    boolean submit(SessionCommand command);

    boolean resolveActivePopup();

    boolean acceptActivePopup();

    boolean declineActivePopup();

    ComputerDecision initiateTrade();
}
