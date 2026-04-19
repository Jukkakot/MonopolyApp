package fi.monopoly.application.session.persistence;

public interface LocalSessionPersistenceUiHooks {
    void showPopup(String message);

    void showPersistenceNotice(String message);
}
