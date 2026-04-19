package fi.monopoly.application.session;

import fi.monopoly.domain.session.SessionState;

public interface SessionHost {
    SessionState currentState();

    void replaceState(SessionState restoredState);
}
