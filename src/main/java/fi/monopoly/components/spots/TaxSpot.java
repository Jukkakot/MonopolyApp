package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.TurnResult;
import lombok.Getter;

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
        String popupText = "You need to pay M" + price + " tax.";
        updateMoney(turnPlayer, -price, popupText, callbackAction);
        return null;
    }

}
