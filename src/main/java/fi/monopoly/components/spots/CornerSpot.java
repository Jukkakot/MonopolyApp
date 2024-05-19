package fi.monopoly.components.spots;

import fi.monopoly.types.TurnResult;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.images.SpotImage;

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
