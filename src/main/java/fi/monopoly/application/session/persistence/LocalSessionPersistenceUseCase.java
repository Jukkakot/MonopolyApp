package fi.monopoly.application.session.persistence;

import fi.monopoly.application.session.SessionHost;
import fi.monopoly.domain.session.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
/**
 * Host-side local persistence use case around the active session host.
 *
 * <p>This performs save/load against the current session authority and returns plain feedback data
 * that the client can render however it wants. That keeps persistence semantics in the host-side
 * seam while leaving popup/sidebar rendering in the client.</p>
 */
@RequiredArgsConstructor
public final class LocalSessionPersistenceUseCase {
    private final SessionPersistenceService sessionPersistenceService;
    private final LocalSessionPathProvider localSessionPathProvider;
    private final Supplier<LocalTime> nowSupplier;
    private final SessionHost sessionHost;

    public LocalSessionPersistenceUseCase(
            SessionPersistenceService sessionPersistenceService,
            SessionHost sessionHost
    ) {
        this(
                sessionPersistenceService,
                new SystemPropertyLocalSessionPathProvider(),
                LocalTime::now,
                sessionHost
        );
    }

    public LocalSessionPersistenceResult saveLocalSession() {
        SessionState sessionState = sessionHost.currentState();
        if (sessionState == null) {
            return new LocalSessionPersistenceResult(
                    false,
                    text("game.popup.saveFailed", "No active session"),
                    null
            );
        }
        Path snapshotPath = localSessionPathProvider.resolvePath();
        try {
            sessionPersistenceService.save(snapshotPath, sessionState);
            String popupMessage = text("game.popup.savedTo", snapshotPath.toAbsolutePath());
            String noticeMessage = text("game.status.savedAt", formattedCurrentTime());
            log.info("Saved local session snapshot to {}", snapshotPath.toAbsolutePath());
            return new LocalSessionPersistenceResult(true, popupMessage, noticeMessage);
        } catch (RuntimeException e) {
            log.error("Failed to save local session snapshot to {}", snapshotPath.toAbsolutePath(), e);
            return new LocalSessionPersistenceResult(
                    false,
                    text("game.popup.saveFailed", e.getMessage()),
                    null
            );
        }
    }

    public LocalSessionPersistenceResult loadLocalSession() {
        Path snapshotPath = localSessionPathProvider.resolvePath();
        if (!sessionPersistenceService.exists(snapshotPath)) {
            return new LocalSessionPersistenceResult(
                    false,
                    text("game.popup.noSaveFound", snapshotPath.toAbsolutePath()),
                    null
            );
        }
        try {
            SessionState restoredState = sessionPersistenceService.load(snapshotPath);
            sessionHost.replaceState(restoredState);
            String popupMessage = text("game.popup.loadedFrom", snapshotPath.toAbsolutePath());
            String noticeMessage = text("game.status.loadedAt", formattedCurrentTime());
            log.info("Loaded local session snapshot from {}", snapshotPath.toAbsolutePath());
            return new LocalSessionPersistenceResult(true, popupMessage, noticeMessage);
        } catch (RuntimeException e) {
            log.error("Failed to load local session snapshot from {}", snapshotPath.toAbsolutePath(), e);
            return new LocalSessionPersistenceResult(
                    false,
                    text("game.popup.loadFailed", e.getMessage()),
                    null
            );
        }
    }

    private String formattedCurrentTime() {
        return nowSupplier.get().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
