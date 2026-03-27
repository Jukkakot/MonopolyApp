package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.TurnResult;
import lombok.Getter;

import static fi.monopoly.text.UiTexts.text;

public class TaxSpot extends Spot {
    @Getter
    private final Integer price;

    public TaxSpot(SpotImage image) {
        super(image);
        price = spotType.getIntegerProperty("price");
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        gameState.getPaymentHandler().requestPayment(
                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, price, text("tax.reason.pay", price)),
                callbackAction
        );
        return null;
    }

}
