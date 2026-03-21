package fi.monopoly.components.cards;

import fi.monopoly.types.CardType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardRecordsTest {

    @Test
    void cardStoresConstructorValues() {
        Card card = new Card(CardType.MOVE, "Advance to Go", List.of("GO_SPOT"));

        assertEquals(CardType.MOVE, card.cardType());
        assertEquals("Advance to Go", card.text());
        assertEquals(List.of("GO_SPOT"), card.values());
    }

    @Test
    void cardInfoStoresConstructorValues() {
        CardInfo cardInfo = new CardInfo(CardType.MONEY, "Bank error in your favor", List.of("200"));

        assertEquals(CardType.MONEY, cardInfo.cardType());
        assertEquals("Bank error in your favor", cardInfo.text());
        assertEquals(List.of("200"), cardInfo.values());
    }
}
