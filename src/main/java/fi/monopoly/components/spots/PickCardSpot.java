package fi.monopoly.components.spots;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.cards.Card;
import fi.monopoly.components.cards.Cards;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.PathMode;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;

import java.util.ArrayList;
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
            case ALL_PLAYERS_MONEY -> handleAllPlayersMoneyCard(gameState, turnPlayer, card, callbackAction);
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

    private void handleAllPlayersMoneyCard(GameState gameState, Player turnPlayer, Card card, CallbackAction callbackAction) {
        int amount = Integer.parseInt(card.values().get(0));
        List<Player> otherPlayers = new ArrayList<>();
        gameState.getPlayers().forEachOtherPLayer(turnPlayer, otherPlayers::add);

        runtime.popupService().show(card.text(), () -> processAllPlayersMoneyPayment(gameState, turnPlayer, otherPlayers, amount, 0, callbackAction));
    }

    private void processAllPlayersMoneyPayment(
            GameState gameState,
            Player turnPlayer,
            List<Player> otherPlayers,
            int amount,
            int index,
            CallbackAction onComplete
    ) {
        if (index >= otherPlayers.size()) {
            onComplete.doAction();
            return;
        }

        Player otherPlayer = otherPlayers.get(index);
        PaymentRequest request = amount >= 0
                ? new PaymentRequest(otherPlayer, new PlayerTarget(turnPlayer), amount, "Card effect: " + spotType.streetType.name())
                : new PaymentRequest(turnPlayer, new PlayerTarget(otherPlayer), -amount, "Card effect: " + spotType.streetType.name());

        gameState.getPaymentHandler().requestPayment(
                request,
                () -> processAllPlayersMoneyPayment(gameState, turnPlayer, otherPlayers, amount, index + 1, onComplete)
        );
    }
}
