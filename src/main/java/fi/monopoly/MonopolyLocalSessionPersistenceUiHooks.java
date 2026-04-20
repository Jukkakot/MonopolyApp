package fi.monopoly;

import fi.monopoly.application.session.persistence.LocalSessionPersistenceUiHooks;
import fi.monopoly.components.popup.components.ButtonProps;

/**
 * Bridges local-session persistence UI callbacks into the current desktop runtime.
 */
final class MonopolyLocalSessionPersistenceUiHooks implements LocalSessionPersistenceUiHooks {
    private final MonopolyApp app;

    MonopolyLocalSessionPersistenceUiHooks(MonopolyApp app) {
        this.app = app;
    }

    @Override
    public void showPopup(String message) {
        MonopolyRuntime.get().popupService().showManualDecision(
                message,
                new ButtonProps(fi.monopoly.text.UiTexts.text("popup.ok.label"), null)
        );
    }

    @Override
    public void showPersistenceNotice(String message) {
        app.clientSessionRef().showPersistenceNotice(message);
    }
}
