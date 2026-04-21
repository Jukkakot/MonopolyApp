package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.client.session.ClientSessionView;

import java.util.Objects;

/**
 * Thin desktop-client orchestrator around the client session seam.
 *
 * <p>{@code MonopolyApp} should not need to know how local save/load or frame advancement map to
 * the underlying session implementation. This controller keeps that client-side orchestration in
 * one place so the app shell stays closer to a pure Processing host.</p>
 */
public final class DesktopClientSessionController implements DesktopClientSessionRuntime {
    private final ClientSession clientSession;
    private final ClientSessionFeedbackSink feedbackSink;

    public DesktopClientSessionController(
            ClientSession clientSession,
            ClientSessionFeedbackSink feedbackSink
    ) {
        this.clientSession = Objects.requireNonNull(clientSession);
        this.feedbackSink = Objects.requireNonNull(feedbackSink);
    }

    @Override
    public void startFreshSession() {
        clientSession.startFreshSession();
    }

    @Override
    public void advanceFrame() {
        clientSession.advanceFrame();
    }

    @Override
    public ClientSessionView currentView() {
        return clientSession.currentView();
    }

    @Override
    public ClientSessionSnapshot currentSnapshot() {
        return clientSession.snapshot();
    }

    @Override
    public void addListener(ClientSessionListener listener) {
        clientSession.addListener(listener);
    }

    @Override
    public void removeListener(ClientSessionListener listener) {
        clientSession.removeListener(listener);
    }

    @Override
    public void saveLocalSession() {
        feedbackSink.showPersistenceResult(clientSession.saveLocalSession());
    }

    @Override
    public void loadLocalSession() {
        feedbackSink.showPersistenceResult(clientSession.loadLocalSession());
    }
}
