package org.example.components;

import org.example.MonopolyApp;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.components.spots.UtilityPropertySpot;

public class GameTurnUtils {
    private static GameTurnUtils instance = null;

    private GameTurnUtils() {}

    public static GameTurnUtils getInstance() {
        if (instance == null) {
            instance = new GameTurnUtils();
        }
        return instance;
    }

    public void handleTurn(Players players, Dices dices, CallbackAction callbackAction) {
        Player turnPlayer = players.getTurn();
        Spot turnPlayerSpot = turnPlayer.getSpot();
        if (turnPlayerSpot instanceof PropertySpot propertySpot) {
            handleSpot(propertySpot, turnPlayer, dices, callbackAction);
        } else {
            callbackAction.doAction();
        }
    }

    private void handleSpot(PropertySpot ps, Player turnPlayer, Dices dices, CallbackAction callbackAction) {
        if (!ps.hasOwner()) {
            handleEmptyProperty(ps, turnPlayer, callbackAction);
        } else if (!ps.isOwner(turnPlayer)) {
            handlePayRent(ps, turnPlayer, dices, callbackAction);
        } else {
            callbackAction.doAction();
        }
    }

    private void handleEmptyProperty(PropertySpot ps, Player turnPlayer, CallbackAction callbackAction) {
        String choiceText = "Arrived at " + ps.getName() + " do you want to buy it?";
        ButtonAction onAccept = () -> {
            if (!turnPlayer.buyProperty(ps)) {
                Popup.showInfo("Didn't have enough money to buy " + ps.getName(), callbackAction::doAction);
            }
        };
        Popup.showChoice(choiceText, onAccept);
    }

    private void handlePayRent(PropertySpot ps, Player turnPlayer, Dices dices, CallbackAction callbackAction) {
        Integer rent;
        if (ps instanceof UtilityPropertySpot utilityPropertySpot) {
            rent = utilityPropertySpot.getRent(turnPlayer, dices);
        } else {
            rent = ps.getRent(turnPlayer);
        }
        String popupText = "Uh oh... you need to pay M" + rent + " rent to " + ps.getOwnerPlayer().getName();
        Popup.showInfo(popupText, () -> {
            if (ps.getOwnerPlayer().giveMoney(turnPlayer, rent)) {
                callbackAction.doAction();
            } else {
                //TODO: handle case if turn player has not enough money
                callbackAction.doAction();
            }
        });
    }
}
