package org.example.components.spots;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.popup.OkPopup;
import org.example.images.SpotImage;
import org.example.types.PathMode;
import org.example.types.SpotType;
import org.example.types.TurnResult;

public class GoToJailSpot extends Spot {

    public GoToJailSpot(SpotImage spotImage) {
        super(spotImage);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        //TODO don't really need to show info, but somehow needs to send result first before actually doing callback action...
        OkPopup.showInfo("Go to jail", callbackAction::doAction);
        return TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .shouldGoToJail(true)
                .build();
    }
}
