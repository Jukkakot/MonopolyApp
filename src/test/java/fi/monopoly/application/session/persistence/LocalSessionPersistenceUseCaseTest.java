package fi.monopoly.application.session.persistence;

import fi.monopoly.application.session.SessionHost;
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
import fi.monopoly.infrastructure.persistence.session.LegacySessionRuntimeRestorer;
import fi.monopoly.infrastructure.persistence.session.SessionSnapshot;
import fi.monopoly.infrastructure.persistence.session.SessionSnapshotMapper;
import fi.monopoly.infrastructure.persistence.session.SessionSnapshotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSessionPersistenceUseCaseTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadReturnHostOwnedFeedbackMessages() {
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
        Path snapshotPath = tempDir.resolve("local-session.json");
        SessionState original = sampleSessionState("session-local");
        RecordingSessionHost sessionHost = new RecordingSessionHost(original);
        LocalSessionPersistenceUseCase useCase = new LocalSessionPersistenceUseCase(
                new SessionPersistenceService(
                        new SessionSnapshotMapper(),
                        new InMemorySnapshotStore(),
                        new LegacySessionRuntimeRestorer()
                ),
                () -> snapshotPath,
                () -> LocalTime.of(9, 30, 15),
                sessionHost
        );

        LocalSessionPersistenceResult saveResult = useCase.saveLocalSession();
        assertTrue(saveResult.success());
        assertEquals("Saved game to " + snapshotPath.toAbsolutePath(), saveResult.popupMessage());
        assertEquals("Saved 09:30:15", saveResult.persistenceNotice());

        sessionHost.currentSessionState = sampleSessionState("mutated");
        LocalSessionPersistenceResult loadResult = useCase.loadLocalSession();

        assertTrue(loadResult.success());
        assertNotNull(sessionHost.rebuiltSessionState);
        assertEquals(original, sessionHost.rebuiltSessionState);
        assertEquals("Loaded game from " + snapshotPath.toAbsolutePath(), loadResult.popupMessage());
        assertEquals("Loaded 09:30:15", loadResult.persistenceNotice());
    }

    @Test
    void loadWithoutSnapshotReturnsInformationalFailureResult() {
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
        Path missingPath = tempDir.resolve("missing-session.json");
        RecordingSessionHost sessionHost = new RecordingSessionHost(sampleSessionState("session-local"));
        LocalSessionPersistenceUseCase useCase = new LocalSessionPersistenceUseCase(
                new SessionPersistenceService(),
                () -> missingPath,
                () -> LocalTime.of(9, 30, 15),
                sessionHost
        );

        LocalSessionPersistenceResult result = useCase.loadLocalSession();

        assertFalse(result.success());
        assertEquals("No saved game found at " + missingPath.toAbsolutePath(), result.popupMessage());
        assertEquals(null, result.persistenceNotice());
    }

    private static SessionState sampleSessionState(String sessionId) {
        return new SessionState(
                sessionId,
                5L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB")),
                List.of(new PlayerSnapshot("player-1", "seat-1", "Eka", 1350, 11, false, false, false, 0, 1, List.of("B1"))),
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
    }

    private static final class RecordingSessionHost implements SessionHost {
        private SessionState currentSessionState;
        private SessionState rebuiltSessionState;

        private RecordingSessionHost(SessionState currentSessionState) {
            this.currentSessionState = currentSessionState;
        }

        @Override
        public SessionState currentState() {
            return currentSessionState;
        }

        @Override
        public void replaceState(SessionState restoredState) {
            rebuiltSessionState = restoredState;
        }
    }

    private static final class InMemorySnapshotStore implements SessionSnapshotStore {
        private SessionSnapshot snapshot;
        private Path writtenPath;

        @Override
        public boolean exists(Path path) {
            return snapshot != null && path.equals(writtenPath);
        }

        @Override
        public void write(Path path, SessionSnapshot snapshot) {
            this.snapshot = snapshot;
            this.writtenPath = path;
        }

        @Override
        public SessionSnapshot read(Path path) {
            return snapshot;
        }
    }
}
