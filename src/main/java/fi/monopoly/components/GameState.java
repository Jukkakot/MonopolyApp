package fi.monopoly.components;

import fi.monopoly.application.session.PropertyPurchaseFlow;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.types.TurnResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GameState {
    Players players;
    Dices dices;
    Board board;
    TurnResult prevTurnResult;
    PaymentHandler paymentHandler;
    PropertyPurchaseFlow propertyPurchaseFlow;

    public GameState(Players players, Dices dices, Board board, TurnResult prevTurnResult, PaymentHandler paymentHandler) {
        this(players, dices, board, prevTurnResult, paymentHandler, null);
    }
}
