package org.example.utils;

import org.example.components.CallbackAction;
import org.example.components.board.Path;
import org.example.components.cards.Card;
import org.example.components.dices.Dices;
import org.example.components.Player;
import org.example.components.Players;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.spots.*;

import java.util.List;

public class GameTurnUtils {

    private static int GO_MONEY_AMOUNT = 200;

    private GameTurnUtils() {
    }

    public static void handleTurn(Players players, Dices dices, Path path, CallbackAction callbackAction) {
        Player turnPlayer = players.getTurn();
        Spot turnPlayerSpot = turnPlayer.getSpot();
        if (turnPlayerSpot instanceof PropertySpot propertySpot) {
            handlePropertySpot(propertySpot, turnPlayer, dices, callbackAction);
        } else if (turnPlayerSpot instanceof TaxSpot taxSpot) {
            handleTaxSpot(taxSpot, turnPlayer, callbackAction);
        } else if (turnPlayerSpot instanceof PickCardSpot pickCardSpot) {
            handlePickCardSpot(pickCardSpot, players, callbackAction);
        } else {
            callbackAction.doAction();
        }

        //TODO handle case when this shouldnt be taken?
        if (path.containsGoSpot()) {
            turnPlayer.updateMoney(GO_MONEY_AMOUNT);
        }
    }

    private static void handlePickCardSpot(PickCardSpot pickCardSpot, Players players, CallbackAction callbackAction) {
        Player turnPlayer = players.getTurn();
        Card card = pickCardSpot.pickCard();
        switch (card.cardType()) {
            case MONEY -> updateMoney(turnPlayer, Integer.parseInt(card.values().get(0)), card.text(), callbackAction);
            case OUT_OF_JAIL -> {
                turnPlayer.addOutOfJailCard();
                Popup.showInfo(card.text(), callbackAction::doAction);
            }
            case ALL_PLAYERS_MONEY -> {
                // Amount is negative if turnplayer gives money to others
                // Amount is positive if turnplayer gets money from others
                int amount = Integer.parseInt(card.values().get(0));
                updateMoney(turnPlayer, amount * (players.count() - 1), card.text(), () -> {
                    players.forEachOtherPLayer(turnPlayer, player -> player.updateMoney(-amount));
                    callbackAction.doAction();
                });
            }
            case REPAIR_PROPERTIES -> {
                List<Integer> repairPrices = card.values().stream().map(Integer::valueOf).toList();
                int housePrice = repairPrices.get(0);
                int hotelPrice = repairPrices.get(1);
                int totalCost = turnPlayer.getHouseCount() * housePrice + turnPlayer.getHotelCount() * hotelPrice;
                updateMoney(turnPlayer, -totalCost, card.text(), callbackAction);
            }
            default -> {
                handlePickCardSpot(pickCardSpot, players, callbackAction);
//                System.out.println("Default card behaviour");
//                callbackAction.doAction();
            }
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
        updateMoney(turnPlayer, -taxAmount, popupText, callbackAction);
    }

    private static void updateMoney(Player player, Integer amount, String popupText, CallbackAction callbackAction) {
        Popup.showInfo(popupText, () -> {
            if (player.updateMoney(amount)) {
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
                //TODO better handling if player wants to sell stuff to afford buying this
                Popup.showInfo("You don't have enough money to buy " + ps.getName(), callbackAction::doAction);
            } else {
                callbackAction.doAction();
            }
        };
        Popup.showChoice(choiceText, onAccept, callbackAction::doAction);
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
            if (ps.payRent(turnPlayer, rent)) {
                callbackAction.doAction();
            } else {
                //TODO: handle case if turn player has not enough money
                callbackAction.doAction();
            }
        });
    }
}
