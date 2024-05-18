package org.example.components.spots;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.dices.DiceValue;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.images.SpotImage;
import org.example.types.DiceState;
import org.example.types.TurnResult;
import org.example.utils.Coordinates;

import java.util.*;

public class JailSpot extends Spot {
    //Player,turn counts left
    public static final Map<Player, Integer> jailTimeLeftMap = new HashMap<>();
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
        Coordinates tokenCoords;
        if (player.isInJail()) {
            tokenCoords = getInJailCoords();
        } else {
            tokenCoords = getPassingByCoords();
        }
        return image.move(tokenCoords);
    }

    private Coordinates getPassingByCoords() {
        int index = playersOnSpot.size() - getPlayersInJail().size();
        return JUST_VISIT_COORDS.get(index % JUST_VISIT_COORDS.size());
    }

    private static Set<Player> getPlayersInJail() {
        return jailTimeLeftMap.keySet();
    }

    private static Coordinates getInJailCoords() {
        int index = getPlayersInJail().size() - 1;
        return IN_JAIL_COORDS.get(index % IN_JAIL_COORDS.size());
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
                    if (turnPlayer.useGetOutOfJailCard() || turnPlayer.addMoney(-GET_OUT_OF_JAIL_FEE)) {
                        String text = "You were not sent to jail";
                        Popup.show(text, callbackAction::doAction);
                    } else {
                        String text = "You didn't have get out of jail card or didin't have M50 to pay";
                        Popup.show(text, () -> sendToJail(turnPlayer, callbackAction));
                    }
                };
                String text = "Do you want to pay M50 or use get out of jail card to get out of jail?";
                Popup.show(text, onAccept, () -> sendToJail(turnPlayer, callbackAction));
            } else {
                sendToJail(turnPlayer, callbackAction);
            }
        }
        return null;
    }

    public void handleInJailTurn(Player player, DiceValue diceValue, CallbackAction onGetOufOfJail, CallbackAction onStayInjail) {
        if (player.isInJail()) {
            if (diceValue.diceState().equals(DiceState.DOUBLES)) {
                releaseFromJail(player, onGetOufOfJail);
            } else {
                tryToGetOufOfJail(player, diceValue, onGetOufOfJail, onStayInjail);
            }
            player.setCoords(getTokenCoords(player));
        } else {
            throw new RuntimeException("Player not in jail?");
        }
    }

    private void sendToJail(Player turnPlayer, CallbackAction callbackAction) {
        jailTimeLeftMap.put(turnPlayer, JAIL_ROUND_NUMBER);
        turnPlayer.setInJail(true);
        turnPlayer.setCoords(getTokenCoords(turnPlayer));
        Popup.show("You were sent to jail", callbackAction::doAction);
    }

    public static void tryToGetOufOfJail(Player player, DiceValue diceValue, CallbackAction onGetOufOfJail, CallbackAction onStayInjail) {
        Integer roundCount = jailTimeLeftMap.get(player);
        if (!player.isInJail() || roundCount < 0) {
            throw new RuntimeException("Error with jail handling!");
        }

        if (diceValue.diceState().equals(DiceState.DOUBLES)) {
            releaseFromJail(player, onGetOufOfJail);
        } else if (roundCount == 1) {
            if(!player.addMoney(-GET_OUT_OF_JAIL_FEE)) {
                System.out.println("Player could not afford paying M50 fine");
                //TODO what if cant afford paying fine?
            }
            releaseFromJail(player, onGetOufOfJail);
        } else {
            jailTimeLeftMap.put(player, jailTimeLeftMap.get(player) - 1);
            Popup.show("You still have " + jailTimeLeftMap.get(player) + " rounds left in jail", onStayInjail::doAction);
        }
    }

    public static void releaseFromJail(Player player, CallbackAction onGetOufOfJail) {
        jailTimeLeftMap.remove(player);
        player.setInJail(false);
        Popup.show("You got out of jail", onGetOufOfJail::doAction);
    }
}
