package org.example.utils;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.popup.Popup;
import org.example.components.spots.Spot;
import org.example.types.TurnResult;

public class GameTurnUtils {

    private static int GO_MONEY_AMOUNT = 200;

    private GameTurnUtils() {
    }

    public static TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        Spot turnPlayerSpot = turnPlayer.getSpot();
        TurnResult turnResult = turnPlayerSpot.handleTurn(gameState, callbackAction);

        if (gameState.getPath().containsGoSpot()) {
            turnPlayer.updateMoney(GO_MONEY_AMOUNT);
        }
        return turnResult;
    }

    public static void updateMoney(Player player, Integer amount, String popupText, CallbackAction callbackAction) {
        Popup.showInfo(popupText, () -> {
            if (player.updateMoney(amount)) {
                callbackAction.doAction();
            } else {
                //TODO what if not enough money
                callbackAction.doAction();
            }
        });
    }
}
