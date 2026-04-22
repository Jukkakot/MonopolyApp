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
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                clientSession,
                frameDriver,
                localSessionControls,
                feedbackSink
        );

        controller.saveLocalSession();

        assertEquals(1, localSessionControls.saveCalls);
        assertSame(localSessionControls.saveResult, feedbackSink.lastResult);
    }

    @Test
    void loadLocalSessionRoutesHostResultToFeedbackSink() {
        RecordingClientSession clientSession = new RecordingClientSession();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                clientSession,
                frameDriver,
                localSessionControls,
                feedbackSink
        );

        controller.loadLocalSession();

        assertEquals(1, localSessionControls.loadCalls);
        assertSame(localSessionControls.loadResult, feedbackSink.lastResult);
    }

    @Test
    void advanceFrameUsesDedicatedDesktopFrameDriver() {
        RecordingClientSession clientSession = new RecordingClientSession();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                clientSession,
                frameDriver,
                localSessionControls,
                feedbackSink
        );

        controller.advanceFrame();

        assertEquals(1, frameDriver.advanceCalls);
    }

    @Test
    void startFreshSessionUsesDedicatedLocalSessionControls() {
        RecordingClientSession clientSession = new RecordingClientSession();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                clientSession,
                frameDriver,
                localSessionControls,
                feedbackSink
        );

        controller.startFreshSession();

        assertEquals(1, localSessionControls.startCalls);
    }

    private static final class RecordingClientSession implements ClientSession {
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

    }

    private static final class RecordingFeedbackSink implements ClientSessionFeedbackSink {
        private LocalSessionPersistenceResult lastResult;

        @Override
        public void showPersistenceResult(LocalSessionPersistenceResult result) {
            lastResult = result;
        }
    }

    private static final class RecordingFrameDriver implements DesktopSessionFrameDriver {
        private int advanceCalls;

        @Override
        public void advanceFrame() {
            advanceCalls++;
        }
    }

    private static final class RecordingLocalSessionControls implements DesktopLocalSessionControls {
        private final LocalSessionPersistenceResult saveResult =
                new LocalSessionPersistenceResult(true, "saved", "saved-notice");
        private final LocalSessionPersistenceResult loadResult =
                new LocalSessionPersistenceResult(true, "loaded", "loaded-notice");
        private int startCalls;
        private int saveCalls;
        private int loadCalls;
        private String lastPersistenceNotice;

        @Override
        public void startFreshSession() {
            startCalls++;
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
        public void showPersistenceNotice(String message) {
            lastPersistenceNotice = message;
        }
    }
}
