package fi.monopoly.client.session.local;

import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.DesktopSessionHostCoordinator;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocalDesktopClientSessionTest {

    @Test
    void listenerReceivesImmediateSnapshotAndPublishesAgainWhenStateIsReplaced() {
        LocalDesktopClientSession clientSession = new LocalDesktopClientSession(
                new DesktopSessionHostCoordinator(new HooksStub())
        );
        AtomicReference<ClientSessionSnapshot> latestSnapshot = new AtomicReference<>();
        AtomicInteger eventCount = new AtomicInteger();

        clientSession.addListener(snapshot -> {
            latestSnapshot.set(snapshot);
            eventCount.incrementAndGet();
        });

        assertEquals(ClientSessionSnapshot.empty(), latestSnapshot.get());
        assertEquals(1, eventCount.get());

        clientSession.replaceState(null);

        assertEquals(2, eventCount.get());
        assertEquals(ClientSessionSnapshot.empty(), latestSnapshot.get());
        assertFalse(latestSnapshot.get().viewAvailable());
    }

    @Test
    void snapshotIsEmptyWhenNoGameExists() {
        LocalDesktopClientSession clientSession = new LocalDesktopClientSession(
                new DesktopSessionHostCoordinator(new HooksStub())
        );

        assertNull(clientSession.currentState());
        assertEquals(ClientSessionSnapshot.empty(), clientSession.snapshot());
    }

    private static final class HooksStub implements DesktopSessionHostCoordinator.Hooks {
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
            return null;
        }

        @Override
        public void flushPendingChanges() {
        }
    }
}
