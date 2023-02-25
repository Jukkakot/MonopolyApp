package org.example.components.spots;

import org.example.components.CallbackAction;
import org.example.components.GameState;
import org.example.components.Player;
import org.example.components.cards.Card;
import org.example.components.cards.Cards;
import org.example.components.popup.Popup;
import org.example.images.SpotImage;
import org.example.types.PathMode;
import org.example.types.SpotType;
import org.example.types.StreetType;
import org.example.types.TurnResult;
import org.example.utils.GameTurnUtils;

import java.util.List;

public class PickCardSpot extends Spot {
    private Cards cards;

    public PickCardSpot(SpotImage image) {
        super(image);
        cards = new Cards(image.getSpotType().streetType);
    }

    public Card pickCard() {
        return cards.getCard();
    }

    @Override
    public TurnResult handleTurn(GameState gameState, CallbackAction callbackAction) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        Card card = pickCard();
        switch (card.cardType()) {
            case MONEY ->
                    GameTurnUtils.updateMoney(turnPlayer, Integer.parseInt(card.values().get(0)), card.text(), callbackAction);
            case OUT_OF_JAIL -> {
                turnPlayer.addOutOfJailCard();
                Popup.showInfo(card.text(), callbackAction::doAction);
            }
            case ALL_PLAYERS_MONEY -> {
                // Amount is negative if turnplayer gives money to others
                // Amount is positive if turnplayer gets money from others
                int amount = Integer.parseInt(card.values().get(0));
                GameTurnUtils.updateMoney(turnPlayer, amount * (gameState.getPlayers().count() - 1), card.text(), () -> {
                    gameState.getPlayers().forEachOtherPLayer(turnPlayer, player -> player.updateMoney(-amount));
                    callbackAction.doAction();
                });
            }
            case REPAIR_PROPERTIES -> {
                List<Integer> repairPrices = card.values().stream().map(Integer::valueOf).toList();
                int housePrice = repairPrices.get(0);
                int hotelPrice = repairPrices.get(1);
                int totalCost = turnPlayer.getHouseCount() * housePrice + turnPlayer.getHotelCount() * hotelPrice;
                GameTurnUtils.updateMoney(turnPlayer, -totalCost, card.text(), callbackAction);
            }
            case MOVE -> {
                SpotType moveToSpotType = SpotType.valueOf(card.values().get(0));
                Popup.showInfo(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(moveToSpotType)
                        .pathMode(PathMode.NORMAL)
                        .build();
            }
            case MOVE_NEAREST -> {
                StreetType moveToStreetType = StreetType.valueOf(card.values().get(0));
                Popup.showInfo(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(moveToStreetType)
                        .pathMode(PathMode.NORMAL)
                        .build();
            }
            case MOVE_BACK_3 -> {
                Popup.showInfo(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(3)
                        .pathMode(PathMode.BACKWARDS)
                        .build();
            }
            case GO_JAIL -> {
                Popup.showInfo(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(SpotType.JAIL)
                        .pathMode(PathMode.FLY)
                        .shouldGoToJail(true)
                        .build();
            }
            default -> {
                System.out.println("Default card behaviour, shouldnt happen");
                handleTurn(gameState, callbackAction);
            }
        }
        return null;
    }
}
