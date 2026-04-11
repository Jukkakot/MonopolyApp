package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeUiBuilderTest {

    private final TradeOfferEvaluator evaluator = new TradeOfferEvaluator();
    private final TradeUiBuilder uiBuilder = new TradeUiBuilder(evaluator);

    @Test
    void tradePopupFooterUsesCombinedDeltaForMultipleProperties() {
        Player proposer = new Player("P1", Color.BLACK, 1500, 1);
        Player recipient = new Player("P2", Color.BLUE, 1500, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(proposer, b1);
        TestObjectFactory.giveProperty(proposer, b2);

        TradeOffer offer = new TradeOffer(
                proposer,
                recipient,
                new TradeSelection(0, java.util.List.of(b1, b2), false),
                TradeSelection.NONE
        );

        String footer = uiBuilder.buildTradePopupView(offer, "Title", "Subtitle", null, null, (draft, editingOfferSide) -> null).footer();
        int recipientDelta = evaluator.estimateNetDeltaForRecipient(offer);
        int proposerDelta = evaluator.estimateNetDeltaForRecipient(offer.reversePerspective());

        assertTrue(footer.contains(proposer.getName() + " " + (proposerDelta >= 0 ? "+" : "") + proposerDelta));
        assertTrue(footer.contains(recipient.getName() + " " + (recipientDelta >= 0 ? "+" : "") + recipientDelta));
    }
}
