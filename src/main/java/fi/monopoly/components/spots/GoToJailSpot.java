package fi.monopoly.components.spots;

import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.images.SpotImage;

public class GoToJailSpot extends Spot {

    public GoToJailSpot(SpotImage spotImage) {
        super(spotImage);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        //TODO don't really need to show info, but somehow needs to send result first before actually doing callback action...
        Popup.show("Go to jail", callbackAction::doAction);
        return TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .shouldGoToJail(true)
                .build();
    }
}
