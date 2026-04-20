package fi.monopoly.application.session.persistence;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.domain.session.SessionState;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
/**
 * Orchestrates local save/load against the current session host.
 *
 * <p>The coordinator is intentionally thin: it resolves the local snapshot path, delegates
 * serialization to {@link SessionPersistenceService}, asks the {@link SessionHost} for the
 * current state or to replace the active one, and forwards user-facing feedback through UI hooks.
 * This keeps local persistence separate from both concrete file storage and concrete game UI.</p>
 */
public final class LocalSessionPersistenceCoordinator {
    private final LocalSessionPersistenceUseCase useCase;
    private final LocalSessionPersistenceUiHooks uiHooks;

    public LocalSessionPersistenceCoordinator(
            SessionPersistenceService sessionPersistenceService,
            SessionHost sessionHost,
            LocalSessionPersistenceUiHooks uiHooks
    ) {
        this(
                sessionPersistenceService,
                new SystemPropertyLocalSessionPathProvider(),
                LocalTime::now,
                sessionHost,
                uiHooks
        );
    }

    LocalSessionPersistenceCoordinator(
            SessionPersistenceService sessionPersistenceService,
            LocalSessionPathProvider localSessionPathProvider,
            Supplier<LocalTime> nowSupplier,
            SessionHost sessionHost,
            LocalSessionPersistenceUiHooks uiHooks
    ) {
        this.useCase = new LocalSessionPersistenceUseCase(
                sessionPersistenceService,
                localSessionPathProvider,
                nowSupplier,
                sessionHost
        );
        this.uiHooks = uiHooks;
    }

    public void saveLocalSession() {
        emit(useCase.saveLocalSession());
    }

    public void loadLocalSession() {
        emit(useCase.loadLocalSession());
    }

    private void emit(LocalSessionPersistenceResult result) {
        if (result.popupMessage() != null) {
            uiHooks.showPopup(result.popupMessage());
        }
        if (result.persistenceNotice() != null) {
            uiHooks.showPersistenceNotice(result.persistenceNotice());
        }
    }
}
