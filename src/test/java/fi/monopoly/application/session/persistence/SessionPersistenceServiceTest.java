package fi.monopoly.application.session.persistence;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
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
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionPersistenceServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsSessionSnapshotAsJson() {
        SessionPersistenceService service = new SessionPersistenceService();
        Path snapshotPath = tempDir.resolve("session.json");
        SessionState original = sampleSessionState("session-1");

        service.save(snapshotPath, original);
        SessionState restored = service.load(snapshotPath);

        assertTrue(java.nio.file.Files.exists(snapshotPath));
        assertEquals(original, restored);
    }

    @Test
    void savesAndLoadsSessionSnapshotThroughAbstractStore() {
        InMemorySnapshotStore store = new InMemorySnapshotStore();
        SessionPersistenceService service = new SessionPersistenceService(
                new SessionSnapshotMapper(),
                store,
                new LegacySessionRuntimeRestorer()
        );
        Path snapshotPath = tempDir.resolve("abstract-store-session.json");
        SessionState original = sampleSessionState("session-abstract");

        service.save(snapshotPath, original);
        SessionState restored = service.load(snapshotPath);

        assertEquals(snapshotPath, store.lastWrittenPath);
        assertNotNull(store.snapshot);
        assertEquals(original, restored);
    }

    @Test
    void restoresLegacyRuntimeObjectsFromSavedSnapshot() {
        SessionPersistenceService service = new SessionPersistenceService();
        Path snapshotPath = tempDir.resolve("session-runtime.json");
        SessionState original = new SessionState(
                "session-2",
                8L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-1", 0, "player-1", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-2", 1, "player-2", SeatKind.BOT, ControlMode.AUTOPLAY, "Toka", "STRONG", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-1", "seat-1", "Eka", 1350, 11, false, false, false, 0, 1, List.of("B1")),
                        new PlayerSnapshot("player-2", "seat-2", "Toka", 900, 10, false, false, true, 2, 0, List.of("RR1"))
                ),
                List.of(
                        new PropertyStateSnapshot("B1", "player-1", false, 2, 0),
                        new PropertyStateSnapshot("RR1", "player-2", true, 0, 0)
                ),
                new TurnState("player-2", TurnPhase.WAITING_FOR_DECISION, false, true),
                null,
                null,
                null,
                null,
                null
        );

        service.save(snapshotPath, original);
        var restored = service.restoreRuntime(initHeadlessRuntime(), snapshotPath);

        assertNotNull(restored.board());
        assertEquals(2, restored.players().count());
        assertEquals("Toka", restored.players().getTurn().getName());
        assertEquals(1350, restored.playersById().get("player-1").getMoneyAmount());
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

    private static final class InMemorySnapshotStore implements SessionSnapshotStore {
        private SessionSnapshot snapshot;
        private Path lastWrittenPath;

        @Override
        public boolean exists(Path path) {
            return snapshot != null && path.equals(lastWrittenPath);
        }

        @Override
        public void write(Path path, SessionSnapshot snapshot) {
            this.lastWrittenPath = path;
            this.snapshot = snapshot;
        }

        @Override
        public SessionSnapshot read(Path path) {
            assertEquals(lastWrittenPath, path);
            assertNotNull(snapshot);
            return snapshot;
        }
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = MonopolyApp.DEFAULT_WINDOW_WIDTH;
        app.height = MonopolyApp.DEFAULT_WINDOW_HEIGHT;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }
}
