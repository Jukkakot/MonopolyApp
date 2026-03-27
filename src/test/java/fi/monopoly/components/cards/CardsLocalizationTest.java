package fi.monopoly.components.cards;

import fi.monopoly.text.UiTexts;
import fi.monopoly.types.CardType;
import fi.monopoly.types.StreetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardsLocalizationTest {

    @AfterEach
    void resetLocale() {
        UiTexts.setLocale(Locale.ENGLISH);
    }

    @Test
    void cardTextUsesActiveLocale() {
        UiTexts.setLocale(new Locale("fi"));

        String text = Cards.getLocalizedCardText(StreetType.CHANCE, CardType.GO_JAIL, 0);

        assertEquals("Mene vankilaan. Mene suoraan vankilaan, ala ohita lahtoruutua, ala nosta M200", text);
    }

    @Test
    void cardTextFallsBackToEnglishWhenLocaleIsMissing() {
        UiTexts.setLocale(new Locale("sv"));

        String text = Cards.getLocalizedCardText(StreetType.COMMUNITY, CardType.MOVE, 0);

        assertEquals("Advance to Go (Collect M200)", text);
    }
}
