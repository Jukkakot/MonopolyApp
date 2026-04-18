package fi.monopoly.application.session.persistence;

import java.nio.file.Path;

public final class LocalSessionSnapshotPath {
    private static final String LOCAL_SAVE_PATH_PROPERTY = "monopoly.localSavePath";
    private static final Path DEFAULT_PATH = Path.of("saves", "local-session.json");

    private LocalSessionSnapshotPath() {
    }

    public static Path resolve() {
        String configured = System.getProperty(LOCAL_SAVE_PATH_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return DEFAULT_PATH;
        }
        return Path.of(configured);
    }
}
