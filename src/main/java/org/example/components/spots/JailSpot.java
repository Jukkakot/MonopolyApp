package org.example.components.spots;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.dices.DiceValue;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.ChoicePopup;
import org.example.components.popup.OkPopup;
import org.example.images.SpotImage;
import org.example.types.DiceState;
import org.example.types.TurnResult;
import org.example.utils.Coordinates;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JailSpot extends Spot {
    //Player,turn counts left
    public static final Map<Player, Integer> playersRoundsLeftMap = new HashMap<>();
    public static final int JAIL_ROUND_NUMBER = 3;
    private static final int GET_OUT_OF_JAIL_FEE = 50;
    private static final float BASE_INDEX = SPOT_W / 2;
    private static final List<Coordinates> JUST_VISIT_COORDS = Arrays.asList(new Coordinates(-BASE_INDEX), new Coordinates(-BASE_INDEX, 0), new Coordinates(-BASE_INDEX, BASE_INDEX),
            new Coordinates(0, BASE_INDEX), new Coordinates(BASE_INDEX, BASE_INDEX));
    private static final List<Coordinates> IN_JAIL_COORDS = Arrays.asList(new Coordinates(0, -BASE_INDEX), new Coordinates(BASE_INDEX, -BASE_INDEX), new Coordinates(0, 0), new Coordinates(BASE_INDEX, 0));

    public JailSpot(SpotImage spotImage) {
        super(spotImage);
    }

    @Override
    public Coordinates getTokenCoords(Player player) {
        int index;
        Coordinates tokenSpot;
        if (player.isInJail()) {
            index = playersRoundsLeftMap.keySet().stream().filter(p -> !p.equals(player)).toList().size();
            tokenSpot = IN_JAIL_COORDS.get(index % IN_JAIL_COORDS.size());
        } else {
            index = playersRoundsLeftMap.keySet().stream().filter(p -> !p.equals(player)).toList().size();
            index = players.size() - index;
            tokenSpot = JUST_VISIT_COORDS.get(index % JUST_VISIT_COORDS.size());
        }
        return image.getCoords().move(tokenSpot);
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        //TODO should figure out better way to check if should go to jail...
        TurnResult prevTurnResult = gameState.getPrevTurnResult();
        boolean shouldNotGoToJail = prevTurnResult == null || !prevTurnResult.isShouldGoToJail();
        if (shouldNotGoToJail) {
            callbackAction.doAction();
        } else {
            if (turnPlayer.hasGetOutOfJailCard() || turnPlayer.getMoney() >= GET_OUT_OF_JAIL_FEE) {
                ButtonAction onAccept = () -> {
                    if (turnPlayer.useGetOutOfJailCard() || turnPlayer.updateMoney(-GET_OUT_OF_JAIL_FEE)) {
                        OkPopup.showInfo("You were not sent to jail", callbackAction::doAction);
                    } else {
                        String text = "You didn't have get out of jail card or didin't have M50 to pay";
                        OkPopup.showInfo(text, () -> sendToJail(turnPlayer, callbackAction));
                    }
                };
                String text = "Do you want to pay M50 or use get out of jail card to get out of jail?";
                ChoicePopup.showChoice(text, onAccept, () -> sendToJail(turnPlayer, callbackAction));
            } else {
                sendToJail(turnPlayer, callbackAction);
            }
        }
        return null;
    }

    private void sendToJail(Player turnPlayer, CallbackAction callbackAction) {
        playersRoundsLeftMap.put(turnPlayer, JAIL_ROUND_NUMBER);
        turnPlayer.setCoords(getTokenCoords(turnPlayer));
        OkPopup.showInfo("You were sent to jail", callbackAction::doAction);
    }

    public static void tryToGetOufOfJail(Player player, DiceValue diceValue, CallbackAction onGetOufOfJail, CallbackAction onStayInjail) {
        Integer roundCount = playersRoundsLeftMap.get(player);
        if (!player.isInJail() || roundCount < 0) {
            throw new RuntimeException("Error with jail handling!");
        }

        if (diceValue.diceState().equals(DiceState.DOUBLES)) {
            releaseFromJail(player, onGetOufOfJail);
        } else if (roundCount == 1) {
            //TODO have to pay M50 fine?
            releaseFromJail(player, onGetOufOfJail);
        } else {
            playersRoundsLeftMap.put(player, playersRoundsLeftMap.get(player) - 1);
            OkPopup.showInfo("You still have " + playersRoundsLeftMap.get(player) + " rounds left in jail", onStayInjail::doAction);
        }
    }

    public static void releaseFromJail(Player player, CallbackAction onGetOufOfJail) {
        playersRoundsLeftMap.remove(player);
        OkPopup.showInfo("You got out of jail", onGetOufOfJail::doAction);
    }
}
