package fi.monopoly.components.spots;

import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.UtilityProperty;
import fi.monopoly.images.SpotImage;

@Slf4j
@ToString(callSuper = true)
public class PropertySpot extends Spot {
    @Getter
    private final Property property;

    public PropertySpot(SpotImage spotImage, SpotType spotType) {
        super(spotImage);
        this.property = PropertyFactory.getProperty(spotType);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        if (!property.hasOwner()) {
            handleNoOwnersTurn(gameState, callbackAction);
        } else if (property.hasOwner() && property.isNotOwner(turnPlayer) && !property.isMortgaged()) {
            handleNotOwnerTurn(gameState, callbackAction);
        } else {
            callbackAction.doAction();
        }
        return null;
    }

    private void handleNoOwnersTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        String choiceText = "Arrived at " + name + " do you want to buy it?";
        ButtonAction onAccept = () -> {
            if (!turnPlayer.buyProperty(this.property)) {
                //TODO better handling if player wants to sell stuff to afford buying this
                Popup.show("You don't have enough money to buy " + name, callbackAction::doAction);
            } else {
                callbackAction.doAction();
            }
        };
        Popup.show(choiceText, onAccept, callbackAction::doAction);
    }

    private void handleNotOwnerTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        Integer rent;
        TurnResult prevTurnResult = gameState.getPrevTurnResult();
        // MOVE_NEAREST cards will only provide StreetType next spot criteria -> different calc rule
        boolean wasMoveNearestCardLast = prevTurnResult != null && prevTurnResult.getNextSpotCriteria() instanceof StreetType;
        if (wasMoveNearestCardLast) {
            if (property instanceof UtilityProperty utilityProperty) {
                Dices dices = gameState.getDices();
                rent = utilityProperty.getMultiplierRent(dices);
            } else {
                rent = Property.MOVE_NEAREST_CARD_MULTIPLIER * property.getRent(turnPlayer);
            }
        } else {
            rent = property.getRent(turnPlayer);
        }
        String popupText = "Uh oh... you need to pay M" + rent + " rent to " + property.getOwnerPlayer().getName();
        Popup.show(popupText, () -> {
            if (property.payRent(turnPlayer, rent)) {
                callbackAction.doAction();
            } else {
                //TODO: handle case if turn player has not enough money
                callbackAction.doAction();
            }
        });
    }
}
