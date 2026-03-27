package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.cards.Card;
import fi.monopoly.components.cards.Cards;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;

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
            case MONEY -> updateMoney(turnPlayer, Integer.parseInt(card.values().get(0)), card.text(), callbackAction);
            case OUT_OF_JAIL -> {
                turnPlayer.addOutOfJailCard(spotType.streetType);
                runtime.popupService().show(card.text(), callbackAction::doAction);
            }
            case ALL_PLAYERS_MONEY -> {
                // Amount is negative if turnplayer gives money to others
                // Amount is positive if turnplayer gets money from others
                int amount = Integer.parseInt(card.values().get(0));
                updateMoney(turnPlayer, amount * (gameState.getPlayers().count() - 1), card.text(), () -> {
                    gameState.getPlayers().forEachOtherPLayer(turnPlayer, player -> player.addMoney(-amount));
                    callbackAction.doAction();
                });
            }
            case REPAIR_PROPERTIES -> {
                List<Integer> repairPrices = card.values().stream().map(Integer::valueOf).toList();
                int housePrice = repairPrices.get(0);
                int hotelPrice = repairPrices.get(1);
                int totalCost = turnPlayer.getTotalHouseCount() * housePrice + turnPlayer.getTotalHotelCount() * hotelPrice;
                updateMoney(turnPlayer, -totalCost, card.text(), callbackAction);
            }
            case MOVE -> {
                SpotType moveToSpotType = SpotType.valueOf(card.values().get(0));
                runtime.popupService().show(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(moveToSpotType)
                        .pathMode(PathMode.NORMAL)
                        .build();
            }
            case MOVE_NEAREST -> {
                StreetType moveToStreetType = StreetType.valueOf(card.values().get(0));
                runtime.popupService().show(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(moveToStreetType)
                        .pathMode(PathMode.NORMAL)
                        .build();
            }
            case MOVE_BACK_3 -> {
                runtime.popupService().show(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(3)
                        .pathMode(PathMode.BACKWARDS)
                        .build();
            }
            case GO_JAIL -> {
                runtime.popupService().show(card.text(), callbackAction::doAction);
                return TurnResult.builder()
                        .nextSpotCriteria(SpotType.JAIL)
                        .pathMode(PathMode.FLY)
                        .shouldGoToJail(true)
                        .build();
            }
            default -> {
                throw new RuntimeException("Default card behaviour, shouldnt happen");
            }
        }
        return null;
    }
}
