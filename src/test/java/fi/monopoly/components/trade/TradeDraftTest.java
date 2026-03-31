package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TradeDraftTest {

    @Test
    void counterOfferSwapsRolesAndSelections() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        TradeSelection offered = new TradeSelection(110, java.util.List.of(new StreetProperty(SpotType.B1)), true);
        TradeSelection requested = new TradeSelection(50, java.util.List.of(new StreetProperty(SpotType.B2)), false);

        TradeDraft counterOffer = new TradeDraft(proposer, recipient, offered, requested).asCounterOffer();

        assertSame(recipient, counterOffer.proposer());
        assertSame(proposer, counterOffer.recipient());
        assertEquals(requested, counterOffer.offeredToRecipient());
        assertEquals(offered, counterOffer.requestedFromRecipient());
    }

    @Test
    void selectionCanAddAndRemoveMultipleProperties() {
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        StreetProperty b2 = new StreetProperty(SpotType.B2);

        TradeSelection selection = TradeSelection.NONE
                .withAddedProperty(b1)
                .withAddedProperty(b2)
                .withRemovedProperty(b1);

        assertEquals(java.util.List.of(b2), selection.properties());
    }
}
