package org.example.components.spots;

import org.example.CallbackAction;
import org.example.components.Card;
import org.example.components.Cards;
import org.example.components.popup.Popup;
import org.example.images.SpotImage;

public class PickCardSpot extends Spot {
    private Cards cards;

    public PickCardSpot(SpotImage image) {
        super(image);
        cards = new Cards(image.getSpotType().streetType);
    }

    public void pickCard(CallbackAction callbackAction) {
        Card card = cards.getCard();
        Popup.showInfo(card.getText(), callbackAction::doAction);
    }
}