package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.popup.TradePopupView;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeUiBuilderTest {

    private final TradeOfferEvaluator evaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder uiBuilder = new TradeUiBuilder(evaluator);

    @Test
    void tradePopupFooterShowsVisibleOfferValuesInsteadOfStrategicDelta() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, b1);
        TestObjectFactory.giveProperty(proposer, b2);

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(50, java.util.List.of(b1, b2), false),
                new TradeSelection(0, java.util.List.of(), true)
        );

        TradePopupView view = uiBuilder.buildTradePopupView(offer, "Title", "Subtitle", null, null, (draft, editingOfferSide) -> null);
        String footer = view.footer();

        assertEquals("Trade value: P1 M170, P2 M60", footer);
    }

    @Test
    void marketValueUsesCombinedPropertyPricesForMultipleProperties() {
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);

        int value = evaluator.estimateSelectionMarketValue(new TradeSelection(50, java.util.List.of(b1, b2), false));

        assertEquals(170, value);
    }

    @Test
    void strategicEvaluatorCanStillShowPositiveValueForBothSides() {
        Player proposer = new Player("Eka", Color.BLACK, 1500, 1);
        Player recipient = new Player("Kolmas", Color.BLUE, 1500, 2);
        var parkPlace = PropertyFactory.getProperty(SpotType.DB1);
        var electricCompany = PropertyFactory.getProperty(SpotType.U1);
        StreetProperty vermont = new StreetProperty(SpotType.LB2);
        StreetProperty boardwalk = new StreetProperty(SpotType.DB2);
        TestObjectFactory.giveProperty(proposer, parkPlace);
        TestObjectFactory.giveProperty(proposer, electricCompany);
        TestObjectFactory.giveProperty(recipient, vermont);
        TestObjectFactory.giveProperty(recipient, boardwalk);

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, java.util.List.of(parkPlace, electricCompany), false),
                new TradeSelection(50, java.util.List.of(vermont, boardwalk), false)
        );

        assertTrue(evaluator.estimateNetDeltaForRecipient(offer) > 0);
        assertTrue(evaluator.estimateNetDeltaForRecipient(offer.reversePerspective()) > 0,
                "Strategic evaluator is subjective, so both sides can rate the same trade positively");
    }
}
