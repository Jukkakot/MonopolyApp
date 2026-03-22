package fi.monopoly.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiTextsTest {

    @Test
    void textReplacesIndexedParameters() {
        assertEquals(
                "Arrived at Baltic Avenue. Do you want to buy it for M60?",
                UiTexts.text("property.offerToBuy", "Baltic Avenue", 60)
        );
    }
}
