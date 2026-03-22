package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentHandler;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.DiceState;
import fi.monopoly.types.TurnResult;
import fi.monopoly.utils.Coordinates;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class JailSpot extends CornerSpot {
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

    private static Set<Player> getPlayersInJail() {
        return jailTimeLeftMap.keySet();
    }

    private static Coordinates getInJailCoords() {
        int index = getPlayersInJail().size() - 1;
        return IN_JAIL_COORDS.get(index % IN_JAIL_COORDS.size());
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

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        //TODO should figure out better way to check if should go to jail...
        TurnResult prevTurnResult = gameState.getPrevTurnResult();
        boolean shouldNotGoToJail = prevTurnResult == null || !prevTurnResult.isShouldGoToJail();
        if (shouldNotGoToJail) {
            callbackAction.doAction();
        } else {
            if (turnPlayer.hasGetOutOfJailCard() || turnPlayer.getMoneyAmount() >= GET_OUT_OF_JAIL_FEE) {
                ButtonAction onAccept = () -> {
                    if (turnPlayer.useGetOutOfJailCard()) {
                        runtime.popupService().show(text("jail.notSent"), callbackAction::doAction);
                    } else if (turnPlayer.getMoneyAmount() >= GET_OUT_OF_JAIL_FEE) {
                        gameState.getPaymentHandler().requestPayment(
                                new PaymentRequest(turnPlayer, BankTarget.INSTANCE, GET_OUT_OF_JAIL_FEE, "Pay M50 to avoid jail"),
                                () -> runtime.popupService().show(text("jail.notSent"), callbackAction::doAction)
                        );
                    } else {
                        runtime.popupService().show(text("jail.noCardOrCash"), () -> sendToJail(turnPlayer, callbackAction));
                    }
                };
                runtime.popupService().show(text("jail.payOrCardPrompt"), onAccept, () -> sendToJail(turnPlayer, callbackAction));
            } else {
                sendToJail(turnPlayer, callbackAction);
            }
        }
        return null;
    }

    public void handleInJailTurn(Player player, DiceValue diceValue, PaymentHandler paymentHandler, CallbackAction onGetOufOfJail, CallbackAction onStayInjail) {
        if (player.isInJail()) {
            if (diceValue.diceState().equals(DiceState.DOUBLES)) {
                releaseFromJail(player, onGetOufOfJail);
            } else {
                tryToGetOufOfJail(player, diceValue, paymentHandler, onGetOufOfJail, onStayInjail);
            }
            player.setCoords(getTokenCoords(player));
        } else {
            throw new RuntimeException("Player not in jail?");
        }
    }

    private void sendToJail(Player turnPlayer, CallbackAction callbackAction) {
        jailTimeLeftMap.put(turnPlayer, JAIL_ROUND_NUMBER);
        turnPlayer.setCoords(getTokenCoords(turnPlayer));
        log.info("Player sent to jail: {}", turnPlayer.getName());
        runtime.popupService().show(text("jail.sent"), callbackAction::doAction);
    }

    public void tryToGetOufOfJail(Player player, DiceValue diceValue, PaymentHandler paymentHandler, CallbackAction onGetOufOfJail, CallbackAction onStayInjail) {
        Integer roundCount = jailTimeLeftMap.get(player);
        if (!player.isInJail() || roundCount < 0) {
            throw new RuntimeException("Error with jail handling!");
        }

        if (diceValue.diceState().equals(DiceState.DOUBLES)) {
            log.info("Player rolled doubles and gets out of jail: {}", player.getName());
            releaseFromJail(player, onGetOufOfJail);
        } else if (roundCount == 1) {
            paymentHandler.requestPayment(
                    new PaymentRequest(player, BankTarget.INSTANCE, GET_OUT_OF_JAIL_FEE, "Pay M50 to get out of jail"),
                    () -> {
                        log.info("Player paid final jail fine and gets out: {}", player.getName());
                        releaseFromJail(player, onGetOufOfJail);
                    }
            );
        } else {
            jailTimeLeftMap.put(player, jailTimeLeftMap.get(player) - 1);
            log.debug("Player remains in jail: {}, roundsLeft={}", player.getName(), jailTimeLeftMap.get(player));
            runtime.popupService().show(text("jail.roundsLeft", jailTimeLeftMap.get(player)), onStayInjail::doAction);
        }
    }

    public void releaseFromJail(Player player, CallbackAction onGetOufOfJail) {
        jailTimeLeftMap.remove(player);
        player.setCoords(getTokenCoords(player));
        log.info("Player released from jail: {}", player.getName());
        runtime.popupService().show(text("jail.gotOut"), onGetOufOfJail::doAction);
    }
}
