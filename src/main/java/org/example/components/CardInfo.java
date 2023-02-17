package org.example.components;

import lombok.Value;

import java.util.List;

@Value
public class CardInfo {
    String text;
    CardType cardType;
    List<String> values;

    public CardInfo(CardType cardType, String text, List<String> values) {
        this.text = text;
        this.cardType = cardType;
        this.values = values;
    }
}
