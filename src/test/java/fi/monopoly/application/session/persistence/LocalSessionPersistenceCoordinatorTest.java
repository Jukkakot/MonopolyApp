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
import fi.monopoly.persistence.session.LegacySessionRuntimeRestorer;
import fi.monopoly.persistence.session.SessionSnapshot;
import fi.monopoly.persistence.session.SessionSnapshotMapper;
import fi.monopoly.persistence.session.SessionSnapshotStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalSessionPersistenceCoordinatorTest {
    @TempDir
    Path tempDir;

    @Test
    void saveAndLoadUseHooksAndSurfaceMessages() {
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
        Path snapshotPath = tempDir.resolve("local-session.json");
        SessionState original = sampleSessionState("session-local");
        RecordingHooks hooks = new RecordingHooks(original);
        LocalSessionPersistenceCoordinator coordinator = new LocalSessionPersistenceCoordinator(
                new SessionPersistenceService(
                        new SessionSnapshotMapper(),
                        new InMemorySnapshotStore(),
                        new LegacySessionRuntimeRestorer()
                ),
                () -> snapshotPath,
                () -> LocalTime.of(9, 30, 15),
                hooks
        );

        coordinator.saveLocalSession();
        assertEquals("Saved game to " + snapshotPath.toAbsolutePath(), hooks.lastPopupMessage);
        assertEquals("Saved 09:30:15", hooks.lastPersistenceNotice);

        hooks.currentSessionState = sampleSessionState("mutated");
        coordinator.loadLocalSession();

        assertNotNull(hooks.rebuiltSessionState);
        assertEquals(original, hooks.rebuiltSessionState);
        assertEquals("Loaded game from " + snapshotPath.toAbsolutePath(), hooks.lastPopupMessage);
        assertEquals("Loaded 09:30:15", hooks.lastPersistenceNotice);
    }

    @Test
    void loadWithoutExistingSnapshotShowsInformationalPopup() {
        fi.monopoly.text.UiTexts.setLocale(Locale.ENGLISH);
        Path missingPath = tempDir.resolve("missing-session.json");
        RecordingHooks hooks = new RecordingHooks(sampleSessionState("session-local"));
        LocalSessionPersistenceCoordinator coordinator = new LocalSessionPersistenceCoordinator(
                new SessionPersistenceService(),
                () -> missingPath,
                () -> LocalTime.of(9, 30, 15),
                hooks
        );

        coordinator.loadLocalSession();

        assertEquals("No saved game found at " + missingPath.toAbsolutePath(), hooks.lastPopupMessage);
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

    private static final class RecordingHooks implements LocalSessionPersistenceCoordinator.Hooks {
        private SessionState currentSessionState;
        private SessionState rebuiltSessionState;
        private String lastPopupMessage;
        private String lastPersistenceNotice;

        private RecordingHooks(SessionState currentSessionState) {
            this.currentSessionState = currentSessionState;
        }

        @Override
        public SessionState currentSessionState() {
            return currentSessionState;
        }

        @Override
        public void rebuildGame(SessionState restoredState) {
            this.rebuiltSessionState = restoredState;
        }

        @Override
        public void showPopup(String message) {
            this.lastPopupMessage = message;
        }

        @Override
        public void showPersistenceNotice(String message) {
            this.lastPersistenceNotice = message;
        }
    }

    private static final class InMemorySnapshotStore implements SessionSnapshotStore {
        private SessionSnapshot snapshot;

        @Override
        public void write(Path path, SessionSnapshot snapshot) {
            this.snapshot = snapshot;
            try {
                Files.createDirectories(path.toAbsolutePath().getParent());
                Files.writeString(path, "test");
            } catch (IOException e) {
                throw new AssertionError("Failed to create backing test file", e);
            }
        }

        @Override
        public SessionSnapshot read(Path path) {
            assertNotNull(snapshot);
            return snapshot;
        }
    }
}
