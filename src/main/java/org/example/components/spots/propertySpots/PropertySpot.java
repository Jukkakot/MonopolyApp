package org.example.components.spots.propertySpots;

import lombok.Getter;
import lombok.Setter;
import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.dices.Dices;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.spots.Spot;
import org.example.images.SpotImage;
import org.example.types.StreetType;
import org.example.types.TurnResult;

import java.util.Arrays;
import java.util.List;

public abstract class PropertySpot extends Spot {
    @Getter
    protected int price;
    @Getter
    @Setter
    protected boolean isMortgaged = false;
    @Getter
    @Setter
    protected Player ownerPlayer;
    protected List<Integer> rentPrices;
    private static final int MOVE_NEAREST_CARD_UTIL_MULTIPLIER = 10;
    private static final int MOVE_NEAREST_CARD_RAILROAD_MULTIPLIER = 2;

    public PropertySpot(SpotImage sp) {
        super(sp);
        price = Integer.parseInt(spotType.getProperty("price"));
        String rentStr = spotType.getProperty("rents");
        if (rentStr != null && !rentStr.equals("")) {
            rentPrices = Arrays.stream(rentStr.split(",")).map(Integer::valueOf).toList();
        }
    }

    public boolean hasOwner() {
        return ownerPlayer != null;
    }

    public boolean isOwner(Player p) {
        return hasOwner() && ownerPlayer.equals(p);
    }

    public boolean payRent(Player player, Integer rent) {
        return ownerPlayer.giveMoney(player, rent);
    }

    public abstract Integer getRent(Player player);

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        if (!hasOwner()) {
            handleEmptyProperty(gameState, callbackAction);
        } else if (!isOwner(turnPlayer)) {
            handlePayRent(gameState, callbackAction);
        } else {
            callbackAction.doAction();
        }
        return null;
    }

    private void handleEmptyProperty(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        String choiceText = "Arrived at " + name + " do you want to buy it?";
        ButtonAction onAccept = () -> {
            if (!turnPlayer.buyProperty(this)) {
                //TODO better handling if player wants to sell stuff to afford buying this
                Popup.showInfo("You don't have enough money to buy " + name, callbackAction::doAction);
            } else {
                callbackAction.doAction();
            }
        };
        Popup.showChoice(choiceText, onAccept, callbackAction::doAction);
    }

    private void handlePayRent(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        Integer rent;
        TurnResult prevTurnResult = gameState.getPrevTurnResult();
        // MOVE_NEAREST cards will only provide StreetType next spot criteria -> different calc rule
        boolean wasMoveNearestCardLast = prevTurnResult != null && prevTurnResult.nextSpotCriteria() instanceof StreetType;
        if (this instanceof UtilityPropertySpot utilityPropertySpot) {
            Dices dices = gameState.getDices();
            if (wasMoveNearestCardLast) {
                rent = MOVE_NEAREST_CARD_UTIL_MULTIPLIER * dices.getValue().value();
            } else {
                rent = utilityPropertySpot.getRent(turnPlayer, dices);
            }
        } else {
            if (wasMoveNearestCardLast) {
                rent = MOVE_NEAREST_CARD_RAILROAD_MULTIPLIER * getRent(turnPlayer);
            } else {
                rent = getRent(turnPlayer);
            }
        }
        String popupText = "Uh oh... you need to pay M" + rent + " rent to " + getOwnerPlayer().getName();
        Popup.showInfo(popupText, () -> {
            if (payRent(turnPlayer, rent)) {
                callbackAction.doAction();
            } else {
                //TODO: handle case if turn player has not enough money
                callbackAction.doAction();
            }
        });
    }

}
