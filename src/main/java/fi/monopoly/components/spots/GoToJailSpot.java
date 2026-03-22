package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.TurnResult;

import static fi.monopoly.text.UiTexts.text;

public class GoToJailSpot extends CornerSpot {

    public GoToJailSpot(SpotImage spotImage) {
        super(spotImage);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        runtime.popupService().show(text("spot.goToJail"), callbackAction::doAction);
        return TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .shouldGoToJail(true)
                .build();
    }
}
