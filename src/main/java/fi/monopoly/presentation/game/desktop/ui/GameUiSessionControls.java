package fi.monopoly.presentation.game.desktop.ui;

import java.util.List;
import java.util.Locale;

/**
 * Client/session control port used by the desktop gameplay UI.
 *
 * <p>These controls are not part of turn flow or board interaction. They cover session-level UI
 * actions such as pause, bot speed, language switching, and local save/load, so keeping them on a
 * dedicated port prevents those concerns from leaking through broader presentation hook
 * interfaces.</p>
 */
public interface GameUiSessionControls {
    List<Locale> supportedLocales();

    Locale currentLocale();

    void switchLanguage(Locale locale);

    void togglePause();

    void cycleBotSpeedMode();

    void saveSession();

    void loadSession();
}
