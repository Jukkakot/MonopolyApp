package fi.monopoly.components.trade;

import fi.monopoly.components.Player;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TradeDraftTest {

    @Test
    void counterOfferSwapsRolesAndSelections() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        TradeSelection offered = new TradeSelection(110, null, true);
        TradeSelection requested = new TradeSelection(50, null, false);

        TradeDraft counterOffer = new TradeDraft(proposer, recipient, offered, requested).asCounterOffer();

        assertSame(recipient, counterOffer.proposer());
        assertSame(proposer, counterOffer.recipient());
        assertEquals(requested, counterOffer.offeredToRecipient());
        assertEquals(offered, counterOffer.requestedFromRecipient());
    }
}
