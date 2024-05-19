package org.example.components.spots;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.example.components.CallbackAction;
import org.example.components.Game;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.dices.Dices;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.popup.components.ButtonProps;
import org.example.components.properties.Property;
import org.example.components.properties.PropertyFactory;
import org.example.components.properties.UtilityProperty;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import org.example.types.StreetType;
import org.example.types.TurnResult;

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

    @Override
    public void onClick() {
        super.onClick();
        if (!property.isOwner(Game.players.getTurn())) {
            return;
        }
        if (Game.players.getTurn().ownsAllStreetProperties(spotType.streetType)) {
            Popup.show("Do you want to buy houses bro",
                    new ButtonProps("Eka", null),
                    new ButtonProps("Toka", null),
                    new ButtonProps("Kolmas", null),
                    new ButtonProps("Neljäs", null));
        } else {
            Popup.show("You can't buy houses unless you own all same street properties");
        }

    }
}
