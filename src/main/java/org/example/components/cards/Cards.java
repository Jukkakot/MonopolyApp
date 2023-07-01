package org.example.components.cards;

import org.example.types.CardType;
import org.example.types.StreetType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;


public class Cards {
    private Properties props = new Properties();
    private List<Card> cardList = new ArrayList<>();
    private Card previousCard;
    private static final String SINGLE_TEXT_DELIMITER = "#";
    private static final String PROP_VALUES_DELIMITER = ";";

    public Cards(StreetType streetType) {
        try {
            props.load(Cards.class.getResourceAsStream("/" + streetType.name() + ".properties"));
            initCards();
        } catch (Exception e) {
            System.err.println("Error loading card properties! " + e.getMessage());
        }
        Collections.shuffle(cardList);
    }

    private void initCards() {
        for (CardType ct : CardType.values()) {
            String propFullText = props.getProperty(ct.name());
            if (propFullText != null) {
                for (String propSingleText : propFullText.split(SINGLE_TEXT_DELIMITER)) {
                    List<String> propParts = new ArrayList<>(Arrays.asList(propSingleText.split(PROP_VALUES_DELIMITER)));
                    String text = propParts.get(0);
                    propParts.remove(0);
                    cardList.add(new Card(ct, text, propParts));
                }
            }
        }
    }

    public Card getCard() {
        if (previousCard == null) {
            previousCard = cardList.get(0);
        } else if (cardList.indexOf(previousCard) == cardList.size() - 1) {
            Collections.shuffle(cardList);
            System.out.println("Shuffling cards...");
            previousCard = cardList.get(0);
        } else {
            int prevCardIndex = cardList.indexOf(previousCard);
            previousCard = cardList.get(prevCardIndex + 1);
        }
        return previousCard;
    }
}
