package org.example.utils;

import org.example.CallbackAction;
import org.example.components.Dices;
import org.example.components.Player;
import org.example.components.Players;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.spots.*;

public class GameTurnUtils {

    private GameTurnUtils() {}

    public static void handleTurn(Players players, Dices dices, CallbackAction callbackAction) {
        Player turnPlayer = players.getTurn();
        Spot turnPlayerSpot = turnPlayer.getSpot();
        if (turnPlayerSpot instanceof PropertySpot propertySpot) {
            handlePropertySpot(propertySpot, turnPlayer, dices, callbackAction);
        } else if (turnPlayerSpot instanceof TaxSpot taxSpot) {
            handleTaxSpot(taxSpot, turnPlayer, callbackAction);
        } else if(turnPlayerSpot instanceof PickCardSpot pickCardSpot) {
          pickCardSpot.pickCard(callbackAction);
        } else {
            callbackAction.doAction();
        }
    }

    private static void handlePropertySpot(PropertySpot ps, Player turnPlayer, Dices dices, CallbackAction callbackAction) {
        if (!ps.hasOwner()) {
            handleEmptyProperty(ps, turnPlayer, callbackAction);
        } else if (!ps.isOwner(turnPlayer)) {
            handlePayRent(ps, turnPlayer, dices, callbackAction);
        } else {
            callbackAction.doAction();
        }
    }

    private static void handleTaxSpot(TaxSpot ts, Player turnPlayer, CallbackAction callbackAction) {
        Integer taxAmount = ts.getPrice();
        String popupText = "You need to pay M" + taxAmount + " tax.";
        Popup.showInfo(popupText, () -> {
            if (turnPlayer.updateMoney(-taxAmount)) {
                callbackAction.doAction();
            } else {
                //TODO what if not enough money
                callbackAction.doAction();
            }
        });
    }

    private static void handleEmptyProperty(PropertySpot ps, Player turnPlayer, CallbackAction callbackAction) {
        String choiceText = "Arrived at " + ps.getName() + " do you want to buy it?";
        ButtonAction onAccept = () -> {
            if (!turnPlayer.buyProperty(ps)) {
                Popup.showInfo("You don't have enough money to buy " + ps.getName(), callbackAction::doAction);
            }
        };
        Popup.showChoice(choiceText, onAccept);
    }

    private static void handlePayRent(PropertySpot ps, Player turnPlayer, Dices dices, CallbackAction callbackAction) {
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
