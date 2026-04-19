package fi.monopoly.application.session.persistence;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.persistence.session.JsonFileSessionSnapshotStore;
import fi.monopoly.persistence.session.LegacySessionRuntimeRestorer;
import fi.monopoly.persistence.session.RestoredLegacySessionRuntime;
import fi.monopoly.persistence.session.SessionSnapshot;
import fi.monopoly.persistence.session.SessionSnapshotMapper;
import fi.monopoly.persistence.session.SessionSnapshotStore;

import java.nio.file.Path;

/**
 * Converts between authoritative {@link SessionState} instances and persisted snapshots.
 *
 * <p>This service owns the mapping between domain session state and storage format and hides the
 * concrete snapshot store behind {@link SessionSnapshotStore}. It also provides the bridge for
 * rebuilding legacy runtime objects from a restored session when the desktop client needs them.</p>
 */
public final class SessionPersistenceService {
    private final SessionSnapshotMapper sessionSnapshotMapper;
    private final SessionSnapshotStore sessionSnapshotStore;
    private final LegacySessionRuntimeRestorer legacySessionRuntimeRestorer;

    public SessionPersistenceService() {
        this(new SessionSnapshotMapper(), new JsonFileSessionSnapshotStore(), new LegacySessionRuntimeRestorer());
    }

    public SessionPersistenceService(
            SessionSnapshotMapper sessionSnapshotMapper,
            SessionSnapshotStore sessionSnapshotStore,
            LegacySessionRuntimeRestorer legacySessionRuntimeRestorer
    ) {
        this.sessionSnapshotMapper = sessionSnapshotMapper;
        this.sessionSnapshotStore = sessionSnapshotStore;
        this.legacySessionRuntimeRestorer = legacySessionRuntimeRestorer;
    }

    public SessionSnapshot save(Path path, SessionState sessionState) {
        SessionSnapshot snapshot = sessionSnapshotMapper.toSnapshot(sessionState);
        sessionSnapshotStore.write(path, snapshot);
        return snapshot;
    }

    public boolean exists(Path path) {
        return sessionSnapshotStore.exists(path);
    }

    public SessionState load(Path path) {
        SessionSnapshot snapshot = sessionSnapshotStore.read(path);
        return sessionSnapshotMapper.fromSnapshot(snapshot);
    }

    public RestoredLegacySessionRuntime restoreRuntime(MonopolyRuntime runtime, Path path) {
        return restoreRuntime(runtime, load(path));
    }

    public RestoredLegacySessionRuntime restoreRuntime(MonopolyRuntime runtime, SessionState sessionState) {
        return legacySessionRuntimeRestorer.restore(runtime, sessionState);
    }
}
