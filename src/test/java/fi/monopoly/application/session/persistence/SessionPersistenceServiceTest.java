package fi.monopoly.application.session.persistence;

import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.ControlMode;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.PropertyStateSnapshot;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SeatState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionPersistenceServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsSessionSnapshotAsJson() {
        SessionPersistenceService service = new SessionPersistenceService();
        Path snapshotPath = tempDir.resolve("session.json");
        SessionState original = new SessionState(
                "session-1",
                5L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka")),
                List.of(new PlayerSnapshot("player-1", "seat-1", "Eka", 1350, 11, false, false, false, 1, List.of("B1"))),
                List.of(new PropertyStateSnapshot("B1", "player-1", false, 2, 0)),
                new TurnState("player-1", TurnPhase.WAITING_FOR_DECISION, false, false),
                new PendingDecision(
                        "decision-1",
                        DecisionType.PROPERTY_PURCHASE,
                        "player-1",
                        List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY),
                        "Buy B2?",
                        new PropertyPurchaseDecisionPayload("B2", "BALTIC AVENUE", 60)
                ),
                null,
                null,
                null,
                null
        );

        service.save(snapshotPath, original);
        SessionState restored = service.load(snapshotPath);

        assertTrue(java.nio.file.Files.exists(snapshotPath));
        assertEquals(original, restored);
    }
}
