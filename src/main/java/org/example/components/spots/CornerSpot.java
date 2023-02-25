package org.example.components.spots;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.images.SpotImage;
import org.example.types.TurnResult;

public class CornerSpot extends Spot {

    public CornerSpot(SpotImage spotImage) {
        super(spotImage);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        callbackAction.doAction();
        return null;
    }
}
