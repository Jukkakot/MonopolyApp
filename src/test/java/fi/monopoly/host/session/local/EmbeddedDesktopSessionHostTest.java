package fi.monopoly.host.session.local;

import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.domain.session.SessionState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmbeddedDesktopSessionHostTest {

    @Test
    void listenerReceivesImmediateSnapshotAndPublishesAgainWhenStateIsReplaced() {
        EmbeddedDesktopSessionHost hostedSession = new EmbeddedDesktopSessionHost(new HooksStub());
        AtomicReference<ClientSessionSnapshot> latestSnapshot = new AtomicReference<>();
        AtomicInteger eventCount = new AtomicInteger();

        hostedSession.addListener(snapshot -> {
            latestSnapshot.set(snapshot);
            eventCount.incrementAndGet();
        });

        assertEquals(ClientSessionSnapshot.empty(), latestSnapshot.get());
        assertEquals(1, eventCount.get());

        hostedSession.replaceState(null);

        assertEquals(2, eventCount.get());
        assertEquals(ClientSessionSnapshot.empty(), latestSnapshot.get());
        assertFalse(latestSnapshot.get().viewAvailable());
    }

    @Test
    void listenerReceivesEmptySnapshotWhenNoGameExists() {
        EmbeddedDesktopSessionHost hostedSession = new EmbeddedDesktopSessionHost(new HooksStub());
        AtomicReference<ClientSessionSnapshot> latestSnapshot = new AtomicReference<>();

        hostedSession.addListener(latestSnapshot::set);

        assertNull(hostedSession.currentState());
        assertEquals(ClientSessionSnapshot.empty(), latestSnapshot.get());
    }

    private static final class HooksStub implements DesktopSessionHostCoordinator.Hooks {
        @Override
        public void shutdownSessionRuntime() {
        }

        @Override
        public void disposeGame(DesktopHostedGame game) {
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
        public DesktopHostedGame createGame(SessionState restoredState) {
            return null;
        }

        @Override
        public void flushPendingChanges() {
        }
    }
}
