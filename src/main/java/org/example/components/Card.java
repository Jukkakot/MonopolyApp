package org.example.components;

public class Card {
    private CardInfo cardInfo;

    public Card(CardInfo cardInfo) {
        this.cardInfo = cardInfo;
    }

    public String getText() {
        return cardInfo.getText();
    }
}
