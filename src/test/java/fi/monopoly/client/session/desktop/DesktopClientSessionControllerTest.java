package fi.monopoly.client.session.desktop;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DesktopClientSessionControllerTest {

    @Test
    void saveLocalSessionRoutesHostResultToFeedbackSink() {
        RecordingClientSession clientSession = new RecordingClientSession();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(clientSession, feedbackSink);

        controller.saveLocalSession();

        assertEquals(1, clientSession.saveCalls);
        assertSame(clientSession.saveResult, feedbackSink.lastResult);
    }

    @Test
    void loadLocalSessionRoutesHostResultToFeedbackSink() {
        RecordingClientSession clientSession = new RecordingClientSession();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(clientSession, feedbackSink);

        controller.loadLocalSession();

        assertEquals(1, clientSession.loadCalls);
        assertSame(clientSession.loadResult, feedbackSink.lastResult);
    }

    private static final class RecordingClientSession implements ClientSession {
        private final LocalSessionPersistenceResult saveResult =
                new LocalSessionPersistenceResult(true, "saved", "saved-notice");
        private final LocalSessionPersistenceResult loadResult =
                new LocalSessionPersistenceResult(true, "loaded", "loaded-notice");
        private int saveCalls;
        private int loadCalls;

        @Override
        public void startFreshSession() {
        }

        @Override
        public void advanceFrame() {
        }

        @Override
        public LocalSessionPersistenceResult saveLocalSession() {
            saveCalls++;
            return saveResult;
        }

        @Override
        public LocalSessionPersistenceResult loadLocalSession() {
            loadCalls++;
            return loadResult;
        }

        @Override
        public ClientSessionView currentView() {
            return null;
        }

        @Override
        public ClientSessionSnapshot snapshot() {
            return ClientSessionSnapshot.empty();
        }

        @Override
        public void addListener(ClientSessionListener listener) {
        }

        @Override
        public void removeListener(ClientSessionListener listener) {
        }

        @Override
        public void showPersistenceNotice(String message) {
        }
    }

    private static final class RecordingFeedbackSink implements ClientSessionFeedbackSink {
        private LocalSessionPersistenceResult lastResult;

        @Override
        public void showPersistenceResult(LocalSessionPersistenceResult result) {
            lastResult = result;
        }
    }
}
