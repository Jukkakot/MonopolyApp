package fi.monopoly.application.session.persistence;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.persistence.session.JsonSessionStore;
import fi.monopoly.persistence.session.LegacySessionRuntimeRestorer;
import fi.monopoly.persistence.session.RestoredLegacySessionRuntime;
import fi.monopoly.persistence.session.SessionSnapshot;
import fi.monopoly.persistence.session.SessionSnapshotMapper;

import java.nio.file.Path;

public final class SessionPersistenceService {
    private final SessionSnapshotMapper sessionSnapshotMapper;
    private final JsonSessionStore jsonSessionStore;
    private final LegacySessionRuntimeRestorer legacySessionRuntimeRestorer;

    public SessionPersistenceService() {
        this(new SessionSnapshotMapper(), new JsonSessionStore(), new LegacySessionRuntimeRestorer());
    }

    public SessionPersistenceService(
            SessionSnapshotMapper sessionSnapshotMapper,
            JsonSessionStore jsonSessionStore,
            LegacySessionRuntimeRestorer legacySessionRuntimeRestorer
    ) {
        this.sessionSnapshotMapper = sessionSnapshotMapper;
        this.jsonSessionStore = jsonSessionStore;
        this.legacySessionRuntimeRestorer = legacySessionRuntimeRestorer;
    }

    public SessionSnapshot save(Path path, SessionState sessionState) {
        SessionSnapshot snapshot = sessionSnapshotMapper.toSnapshot(sessionState);
        jsonSessionStore.write(path, snapshot);
        return snapshot;
    }

    public SessionState load(Path path) {
        SessionSnapshot snapshot = jsonSessionStore.read(path);
        return sessionSnapshotMapper.fromSnapshot(snapshot);
    }

    public RestoredLegacySessionRuntime restoreRuntime(MonopolyRuntime runtime, Path path) {
        return restoreRuntime(runtime, load(path));
    }

    public RestoredLegacySessionRuntime restoreRuntime(MonopolyRuntime runtime, SessionState sessionState) {
        return legacySessionRuntimeRestorer.restore(runtime, sessionState);
    }
}
