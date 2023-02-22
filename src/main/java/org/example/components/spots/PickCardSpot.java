package org.example.components.spots;

import org.example.components.cards.Card;
import org.example.components.cards.Cards;
import org.example.images.SpotImage;

public class PickCardSpot extends Spot {
    private Cards cards;

    public PickCardSpot(SpotImage image) {
        super(image);
        cards = new Cards(image.getSpotType().streetType);
    }

    public Card pickCard() {
       return cards.getCard();
    }
}
