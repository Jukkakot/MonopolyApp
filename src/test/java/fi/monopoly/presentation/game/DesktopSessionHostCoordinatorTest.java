package fi.monopoly.presentation.game;

import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DesktopSessionHostCoordinatorTest {

    @Test
    void rebuildFreshGameCreatesNewGameAndFlushesPendingChanges() {
        AtomicInteger shutdownCalls = new AtomicInteger();
        AtomicInteger initializeCalls = new AtomicInteger();
        AtomicInteger flushCalls = new AtomicInteger();
        AtomicReference<SessionState> createdState = new AtomicReference<>();

        DesktopSessionHostCoordinator coordinator = new DesktopSessionHostCoordinator(new DesktopSessionHostCoordinator.Hooks() {
            @Override
            public void shutdownSessionRuntime() {
                shutdownCalls.incrementAndGet();
            }

            @Override
            public void disposeGame(Game game) {
            }

            @Override
            public void disposeControlLayer() {
            }

            @Override
            public void initializeControlLayer() {
                initializeCalls.incrementAndGet();
            }

            @Override
            public void applyDefaultTextFont() {
            }

            @Override
            public Game createGame(SessionState restoredState) {
                createdState.set(restoredState);
                return null;
            }

            @Override
            public void flushPendingChanges() {
                flushCalls.incrementAndGet();
            }
        });

        coordinator.rebuildFreshGame();

        assertNull(coordinator.currentGame());
        assertNull(createdState.get());
        assertEquals(2, shutdownCalls.get());
        assertEquals(1, initializeCalls.get());
        assertEquals(1, flushCalls.get());
    }

    @Test
    void replaceStatePausesInProgressSessionBeforeCreatingGame() {
        AtomicReference<SessionState> createdState = new AtomicReference<>();
        DesktopSessionHostCoordinator coordinator = new DesktopSessionHostCoordinator(new HooksStub(createdState));
        SessionState inProgress = new SessionState(
                "session-1",
                5L,
                SessionStatus.IN_PROGRESS,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        coordinator.replaceState(inProgress);

        assertNotNull(createdState.get());
        assertEquals(SessionStatus.PAUSED, createdState.get().status());
        assertEquals(inProgress.sessionId(), createdState.get().sessionId());
        assertEquals(inProgress.version(), createdState.get().version());
    }

    @Test
    void replaceStateKeepsNonRunningSessionStatusAsIs() {
        AtomicReference<SessionState> createdState = new AtomicReference<>();
        DesktopSessionHostCoordinator coordinator = new DesktopSessionHostCoordinator(new HooksStub(createdState));
        SessionState pausedState = new SessionState(
                "session-2",
                9L,
                SessionStatus.PAUSED,
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        coordinator.replaceState(pausedState);

        assertSame(pausedState, createdState.get());
    }

    private static final class HooksStub implements DesktopSessionHostCoordinator.Hooks {
        private final AtomicReference<SessionState> createdState;

        private HooksStub(AtomicReference<SessionState> createdState) {
            this.createdState = createdState;
        }

        @Override
        public void shutdownSessionRuntime() {
        }

        @Override
        public void disposeGame(Game game) {
        }

        @Override
        public void disposeControlLayer() {
        }

        @Override
        public void initializeControlLayer() {
        }

        @Override
        public void applyDefaultTextFont() {
        }

        @Override
        public Game createGame(SessionState restoredState) {
            createdState.set(restoredState);
            return null;
        }

        @Override
        public void flushPendingChanges() {
        }
    }
}
