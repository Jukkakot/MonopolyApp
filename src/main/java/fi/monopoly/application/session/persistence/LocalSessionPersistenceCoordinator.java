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
public final class LocalSessionPersistenceCoordinator {
    private final SessionPersistenceService sessionPersistenceService;
    private final LocalSessionPathProvider localSessionPathProvider;
    private final Supplier<LocalTime> nowSupplier;
    private final SessionHost sessionHost;
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
        this.sessionPersistenceService = sessionPersistenceService;
        this.localSessionPathProvider = localSessionPathProvider;
        this.nowSupplier = nowSupplier;
        this.sessionHost = sessionHost;
        this.uiHooks = uiHooks;
    }

    public void saveLocalSession() {
        SessionState sessionState = sessionHost.currentState();
        if (sessionState == null) {
            return;
        }
        Path snapshotPath = localSessionPathProvider.resolvePath();
        try {
            sessionPersistenceService.save(snapshotPath, sessionState);
            uiHooks.showPopup(text("game.popup.savedTo", snapshotPath.toAbsolutePath()));
            uiHooks.showPersistenceNotice(text("game.status.savedAt", formattedCurrentTime()));
            log.info("Saved local session snapshot to {}", snapshotPath.toAbsolutePath());
        } catch (RuntimeException e) {
            log.error("Failed to save local session snapshot to {}", snapshotPath.toAbsolutePath(), e);
            uiHooks.showPopup(text("game.popup.saveFailed", e.getMessage()));
        }
    }

    public void loadLocalSession() {
        Path snapshotPath = localSessionPathProvider.resolvePath();
        if (!sessionPersistenceService.exists(snapshotPath)) {
            uiHooks.showPopup(text("game.popup.noSaveFound", snapshotPath.toAbsolutePath()));
            return;
        }
        try {
            SessionState restoredState = sessionPersistenceService.load(snapshotPath);
            sessionHost.replaceState(restoredState);
            uiHooks.showPopup(text("game.popup.loadedFrom", snapshotPath.toAbsolutePath()));
            uiHooks.showPersistenceNotice(text("game.status.loadedAt", formattedCurrentTime()));
            log.info("Loaded local session snapshot from {}", snapshotPath.toAbsolutePath());
        } catch (RuntimeException e) {
            log.error("Failed to load local session snapshot from {}", snapshotPath.toAbsolutePath(), e);
            uiHooks.showPopup(text("game.popup.loadFailed", e.getMessage()));
        }
    }

    private String formattedCurrentTime() {
        return nowSupplier.get().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
