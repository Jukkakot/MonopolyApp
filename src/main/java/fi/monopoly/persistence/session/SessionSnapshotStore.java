package fi.monopoly.persistence.session;

import java.nio.file.Path;

public interface SessionSnapshotStore {
    void write(Path path, SessionSnapshot snapshot);

    SessionSnapshot read(Path path);
}
