package fi.monopoly.components.cards;

import fi.monopoly.types.CardType;
import fi.monopoly.types.StreetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardsDeckTest {

    @BeforeEach
    void setUp() {
        Cards.resetDecks();
    }

    @AfterEach
    void resetDecks() {
        Cards.resetDecks();
    }

    @Test
    void outOfJailCardReturnsToDeckAfterBeingReturned() {
        Cards cards = new Cards(StreetType.CHANCE);

        assertEquals(1, drawUntilOutOfJail(cards, 32));
        assertEquals(0, drawUntilOutOfJail(cards, 32));

        Cards.returnOutOfJailCard(StreetType.CHANCE);

        assertEquals(1, drawUntilOutOfJail(cards, 32));
    }

    private int drawUntilOutOfJail(Cards cards, int draws) {
        int hits = 0;
        for (int i = 0; i < draws; i++) {
            if (cards.getCard().cardType() == CardType.OUT_OF_JAIL) {
                hits++;
            }
        }
        return hits;
    }
}
