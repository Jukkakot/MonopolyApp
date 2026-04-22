package fi.monopoly.client.desktop;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.application.session.persistence.LocalSessionPersistenceResult;
import fi.monopoly.client.session.ClientSessionFeedbackSink;
import fi.monopoly.client.session.desktop.DesktopLocalSessionControls;
import fi.monopoly.components.popup.components.ButtonProps;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Renders local persistence feedback into the current desktop runtime.
 */
final class LocalSessionPersistenceUiHooks implements ClientSessionFeedbackSink {
    private final DesktopLocalSessionControls localSessionControls;
    private final Supplier<MonopolyRuntime> runtimeSupplier;

    LocalSessionPersistenceUiHooks(DesktopLocalSessionControls localSessionControls, Supplier<MonopolyRuntime> runtimeSupplier) {
        this.localSessionControls = Objects.requireNonNull(localSessionControls);
        this.runtimeSupplier = Objects.requireNonNull(runtimeSupplier);
    }

    public void showPopup(String message) {
        runtimeSupplier.get().popupService().showManualDecision(
                message,
                new ButtonProps(fi.monopoly.text.UiTexts.text("popup.ok.label"), null)
        );
    }

    public void showPersistenceNotice(String message) {
        localSessionControls.showPersistenceNotice(message);
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
