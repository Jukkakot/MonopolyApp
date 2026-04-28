package fi.monopoly.presentation.session.purchase;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PendingDecisionPopupAdapterTest {

    private static SessionState emptyState() {
        return new SessionState(
                "local-session",
                0L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-0", 0, "player-1", SeatKind.BOT, ControlMode.MANUAL, "Bot", "STRONG", "#000000")),
                List.of(),
                List.of(),
                new TurnState("player-1", TurnPhase.WAITING_FOR_ROLL, true, false),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void syncDismissesStalePropertyOfferWhenPendingDecisionIsGone() {
        FakePopupService popupService = new FakePopupService("propertyOffer");
        SessionApplicationService sessionApplicationService = new SessionApplicationService(
                "local-session",
                () -> emptyState()
        );
        PendingDecisionPopupAdapter adapter = new PendingDecisionPopupAdapter(
                "local-session",
                sessionApplicationService,
                popupService,
                (playerId, propertyId, displayName, price, message, continuationState) -> {
                    throw new UnsupportedOperationException("Not needed in this test");
                },
                () -> {
                },
                playerId -> null
        );

        adapter.sync();

        assertEquals(1, popupService.dismissCalls);
        assertEquals("propertyOffer", popupService.lastDismissedKind);
    }

    private static final class FakePopupService extends PopupService {
        private final String activePopupKind;
        private int dismissCalls;
        private String lastDismissedKind;

        private FakePopupService(String activePopupKind) {
            super(null);
            this.activePopupKind = activePopupKind;
        }

        @Override
        public String activePopupKind() {
            return activePopupKind;
        }

        @Override
        public boolean dismissActivePopup(String popupKind) {
            dismissCalls++;
            lastDismissedKind = popupKind;
            return true;
        }
    }
}
