package fi.monopoly.client.session.desktop;

import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.ClientSessionView;

import java.util.Objects;

/**
 * Thin desktop-client orchestrator around the client session seam.
 *
 * <p>{@code MonopolyApp} should not need to know how local save/load or frame advancement map to
 * the underlying session implementation. This controller keeps that client-side orchestration in
 * one place so the app shell stays closer to a pure Processing host.</p>
 */
public final class DesktopClientSessionController {
    private final ClientSession clientSession;
    private final ClientSessionFeedbackSink feedbackSink;

    public DesktopClientSessionController(
            ClientSession clientSession,
            ClientSessionFeedbackSink feedbackSink
    ) {
        this.clientSession = Objects.requireNonNull(clientSession);
        this.feedbackSink = Objects.requireNonNull(feedbackSink);
    }

    public void startFreshSession() {
        clientSession.startFreshSession();
    }

    public void advanceFrame() {
        clientSession.advanceFrame();
    }

    public ClientSessionView currentView() {
        return clientSession.currentView();
    }

    public void saveLocalSession() {
        feedbackSink.showPersistenceResult(clientSession.saveLocalSession());
    }

    public void loadLocalSession() {
        feedbackSink.showPersistenceResult(clientSession.loadLocalSession());
    }
}
