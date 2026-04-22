package fi.monopoly.client.session.desktop;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionUpdates;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class DesktopClientSessionControllerTest {

    @Test
    void saveLocalSessionRoutesHostResultToFeedbackSink() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        controller.saveLocalSession();

        assertEquals(1, localSessionControls.saveCalls);
        assertSame(localSessionControls.saveResult, feedbackSink.lastResult);
    }

    @Test
    void loadLocalSessionRoutesHostResultToFeedbackSink() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        controller.loadLocalSession();

        assertEquals(1, localSessionControls.loadCalls);
        assertSame(localSessionControls.loadResult, feedbackSink.lastResult);
    }

    @Test
    void advanceFrameUsesDedicatedDesktopFrameDriver() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        controller.advanceFrame();

        assertEquals(1, frameDriver.advanceCalls);
        assertSame(viewPort.view, renderModel.currentView());
    }

    @Test
    void startFreshSessionUsesDedicatedLocalSessionControls() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        controller.startFreshSession();

        assertEquals(1, localSessionControls.startCalls);
        assertSame(viewPort.view, renderModel.currentView());
    }

    @Test
    void constructorSeedsClientOwnedRenderModelFromDedicatedDesktopViewPort() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        assertSame(viewPort.view, renderModel.currentView());
    }

    @Test
    void loadLocalSessionRefreshesClientOwnedRenderModel() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();
        DesktopClientSessionController controller = new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        controller.loadLocalSession();

        assertSame(viewPort.view, renderModel.currentView());
    }

    @Test
    void constructorRegistersClientOwnedSessionModelAsSnapshotListener() {
        RecordingClientSessionUpdates sessionUpdates = new RecordingClientSessionUpdates();
        RecordingFrameDriver frameDriver = new RecordingFrameDriver();
        RecordingViewPort viewPort = new RecordingViewPort();
        DesktopClientSessionModel sessionModel = new DesktopClientSessionModel();
        DesktopClientRenderModel renderModel = new DesktopClientRenderModel();
        RecordingLocalSessionControls localSessionControls = new RecordingLocalSessionControls();
        RecordingFeedbackSink feedbackSink = new RecordingFeedbackSink();

        new DesktopClientSessionController(
                sessionUpdates,
                frameDriver,
                viewPort,
                sessionModel,
                renderModel,
                localSessionControls,
                feedbackSink
        );

        assertEquals(1, sessionUpdates.addListenerCalls);
        assertSame(sessionUpdates.lastAddedListener, sessionModel);
        assertEquals(sessionUpdates.snapshot, sessionModel.currentSnapshot());
    }

    private static final class RecordingClientSessionUpdates implements ClientSessionUpdates {
        private final ClientSessionSnapshot snapshot = new ClientSessionSnapshot("session-1", 3L, null, true);
        private int addListenerCalls;
        private ClientSessionListener lastAddedListener;

        @Override
        public void addListener(ClientSessionListener listener) {
            addListenerCalls++;
            lastAddedListener = listener;
            listener.onSnapshotChanged(snapshot);
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

    private static final class RecordingViewPort implements DesktopSessionViewPort {
        private final DesktopSessionRenderView view = new DesktopSessionRenderView() {
            @Override
            public void draw() {
            }

            @Override
            public java.util.List<String> debugPerformanceLines(float fps) {
                return java.util.List.of();
            }
        };

        @Override
        public DesktopSessionRenderView currentView() {
            return view;
        }
    }
}
