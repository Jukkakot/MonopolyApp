package fi.monopoly;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.components.popup.components.ButtonProps;

/**
 * Renders local persistence feedback into the current desktop runtime.
 */
final class MonopolyLocalSessionPersistenceUiHooks {
    private final MonopolyApp app;

    MonopolyLocalSessionPersistenceUiHooks(MonopolyApp app) {
        this.app = app;
    }

    public void showPopup(String message) {
        MonopolyRuntime.get().popupService().showManualDecision(
                message,
                new ButtonProps(fi.monopoly.text.UiTexts.text("popup.ok.label"), null)
        );
    }

    public void showPersistenceNotice(String message) {
        app.clientSessionRef().showPersistenceNotice(message);
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
}
