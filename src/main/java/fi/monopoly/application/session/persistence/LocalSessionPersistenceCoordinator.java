package fi.monopoly.application.session.persistence;

import fi.monopoly.domain.session.SessionState;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
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
    private final Hooks hooks;

    public LocalSessionPersistenceCoordinator(SessionPersistenceService sessionPersistenceService, Hooks hooks) {
        this(sessionPersistenceService, new SystemPropertyLocalSessionPathProvider(), LocalTime::now, hooks);
    }

    LocalSessionPersistenceCoordinator(
            SessionPersistenceService sessionPersistenceService,
            LocalSessionPathProvider localSessionPathProvider,
            Supplier<LocalTime> nowSupplier,
            Hooks hooks
    ) {
        this.sessionPersistenceService = sessionPersistenceService;
        this.localSessionPathProvider = localSessionPathProvider;
        this.nowSupplier = nowSupplier;
        this.hooks = hooks;
    }

    public void saveLocalSession() {
        SessionState sessionState = hooks.currentSessionState();
        if (sessionState == null) {
            return;
        }
        Path snapshotPath = localSessionPathProvider.resolvePath();
        try {
            sessionPersistenceService.save(snapshotPath, sessionState);
            hooks.showPopup(text("game.popup.savedTo", snapshotPath.toAbsolutePath()));
            hooks.showPersistenceNotice(text("game.status.savedAt", formattedCurrentTime()));
            log.info("Saved local session snapshot to {}", snapshotPath.toAbsolutePath());
        } catch (RuntimeException e) {
            log.error("Failed to save local session snapshot to {}", snapshotPath.toAbsolutePath(), e);
            hooks.showPopup(text("game.popup.saveFailed", e.getMessage()));
        }
    }

    public void loadLocalSession() {
        Path snapshotPath = localSessionPathProvider.resolvePath();
        if (!Files.exists(snapshotPath)) {
            hooks.showPopup(text("game.popup.noSaveFound", snapshotPath.toAbsolutePath()));
            return;
        }
        try {
            SessionState restoredState = sessionPersistenceService.load(snapshotPath);
            hooks.rebuildGame(restoredState);
            hooks.showPopup(text("game.popup.loadedFrom", snapshotPath.toAbsolutePath()));
            hooks.showPersistenceNotice(text("game.status.loadedAt", formattedCurrentTime()));
            log.info("Loaded local session snapshot from {}", snapshotPath.toAbsolutePath());
        } catch (RuntimeException e) {
            log.error("Failed to load local session snapshot from {}", snapshotPath.toAbsolutePath(), e);
            hooks.showPopup(text("game.popup.loadFailed", e.getMessage()));
        }
    }

    private String formattedCurrentTime() {
        return nowSupplier.get().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public interface Hooks {
        SessionState currentSessionState();

        void rebuildGame(SessionState restoredState);

        void showPopup(String message);

        void showPersistenceNotice(String message);
    }
}
