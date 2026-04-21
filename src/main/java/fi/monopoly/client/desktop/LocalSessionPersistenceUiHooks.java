package fi.monopoly.client.desktop;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSession;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.components.popup.components.ButtonProps;

/**
 * Renders local persistence feedback into the current desktop runtime.
 */
final class LocalSessionPersistenceUiHooks implements ClientSessionFeedbackSink {
    private final ClientSession clientSession;

    LocalSessionPersistenceUiHooks(ClientSession clientSession) {
        this.clientSession = clientSession;
    }

    public void showPopup(String message) {
        MonopolyRuntime.get().popupService().showManualDecision(
                message,
                new ButtonProps(fi.monopoly.text.UiTexts.text("popup.ok.label"), null)
        );
    }

    public void showPersistenceNotice(String message) {
        clientSession.showPersistenceNotice(message);
    }

    public void showResult(LocalSessionPersistenceResult result) {
        if (result == null) {
            return;
        }
        if (result.popupMessage() != null) {
            showPopup(result.popupMessage());
        }
        if (result.persistenceNotice() != null) {
            showPersistenceNotice(result.persistenceNotice());
        }
    }

    @Override
    public void showPersistenceResult(LocalSessionPersistenceResult result) {
        showResult(result);
    }
}
