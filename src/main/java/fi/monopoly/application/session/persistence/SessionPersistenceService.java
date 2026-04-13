package fi.monopoly.application.session.persistence;

import fi.monopoly.domain.session.SessionState;
import fi.monopoly.persistence.session.JsonSessionStore;
import fi.monopoly.persistence.session.SessionSnapshot;
import fi.monopoly.persistence.session.SessionSnapshotMapper;

import java.nio.file.Path;

public final class SessionPersistenceService {
    private final SessionSnapshotMapper sessionSnapshotMapper;
    private final JsonSessionStore jsonSessionStore;

    public SessionPersistenceService() {
        this(new SessionSnapshotMapper(), new JsonSessionStore());
    }

    public SessionPersistenceService(SessionSnapshotMapper sessionSnapshotMapper, JsonSessionStore jsonSessionStore) {
        this.sessionSnapshotMapper = sessionSnapshotMapper;
        this.jsonSessionStore = jsonSessionStore;
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
}
