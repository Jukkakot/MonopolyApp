package fi.monopoly.persistence.session;

import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.ControlMode;
import fi.monopoly.domain.session.DebtAction;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.PropertyStateSnapshot;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SeatState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.session.TradeHistoryEntry;
import fi.monopoly.domain.session.TradeOfferState;
import fi.monopoly.domain.session.TradeSelectionState;
import fi.monopoly.domain.session.TradeState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionSnapshotMapperTest {
    private final SessionSnapshotMapper mapper = new SessionSnapshotMapper();

    @Test
    void roundTripsFullSessionStateIncludingSubsystemStates() {
        SessionState original = new SessionState(
                "session-1",
                42L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.BOT, ControlMode.AUTOPLAY, "Toka", "STRONG", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1500, 7, false, false, false, 0, 1, List.of("B1", "B2")),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 1200, 12, false, false, true, 2, 0, List.of("RR1"))
                ),
                List.of(
                        new PropertyStateSnapshot("B1", "player-1", false, 1, 0),
                        new PropertyStateSnapshot("B2", "player-1", true, 0, 0),
                        new PropertyStateSnapshot("RR1", "player-2", false, 0, 0)
                ),
                new TurnState("player-1", TurnPhase.WAITING_FOR_DECISION, false, true),
                new PendingDecision(
                        "decision-1",
                        DecisionType.PROPERTY_PURCHASE,
                        "player-1",
                        List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY),
                        "Buy WATER WORKS?",
                        new PropertyPurchaseDecisionPayload("U2", "WATER WORKS", 150)
                ),
                new AuctionState(
                        "auction-1",
                        "U2",
                        "player-1",
                        "player-2",
                        "player-2",
                        90,
                        100,
                        Set.of("player-1"),
                        List.of("player-1", "player-2"),
                        AuctionStatus.ACTIVE,
                        0,
                        null
                ),
                new DebtStateModel(
                        "debt-1",
                        "player-2",
                        DebtCreditorType.BANK,
                        null,
                        200,
                        "Pay bank",
                        true,
                        50,
                        180,
                        List.of(DebtAction.MORTGAGE_PROPERTY, DebtAction.DECLARE_BANKRUPTCY)
                ),
                new TradeState(
                        "trade-1",
                        "player-1",
                        "player-2",
                        TradeStatus.COUNTERED,
                        new TradeOfferState(
                                "player-1",
                                "player-2",
                                new TradeSelectionState(100, List.of("B1"), 0),
                                new TradeSelectionState(0, List.of("RR1"), 1)
                        ),
                        "player-2",
                        true,
                        "player-1",
                        "player-1",
                        List.of(new TradeHistoryEntry("player-2", "COUNTERED", "Countered for RR1"))
                ),
                null
        );

        SessionSnapshot snapshot = mapper.toSnapshot(original);
        SessionState restored = mapper.fromSnapshot(snapshot);

        assertEquals(original, restored);
    }

    @Test
    void rejectsUnknownSnapshotSchemaVersion() {
        SessionSnapshot snapshot = new SessionSnapshot(
                999,
                "session-1",
                1L,
                SessionStatus.IN_PROGRESS,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(IllegalArgumentException.class, () -> mapper.fromSnapshot(snapshot));
    }
}
