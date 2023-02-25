package org.example.components.spots;

import lombok.Getter;
import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.images.SpotImage;
import org.example.types.TurnResult;
import org.example.utils.GameTurnUtils;

public class TaxSpot extends Spot {
    @Getter
    private final Integer price;

    public TaxSpot(SpotImage image) {
        super(image);
        price = Integer.parseInt(spotType.getProperty("price"));
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        String popupText = "You need to pay M" + price + " tax.";
        GameTurnUtils.updateMoney(turnPlayer, -price, popupText, callbackAction);
        return null;
    }

}
