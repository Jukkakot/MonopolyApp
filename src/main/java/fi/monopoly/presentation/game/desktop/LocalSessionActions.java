package fi.monopoly.presentation.game.desktop;

public record LocalSessionActions(
        Runnable saveSession,
        Runnable loadSession
) {
    private static final Runnable NO_OP = () -> { };
    public static final LocalSessionActions NO_OP_ACTIONS = new LocalSessionActions(NO_OP, NO_OP);

    public LocalSessionActions {
        saveSession = saveSession != null ? saveSession : NO_OP;
        loadSession = loadSession != null ? loadSession : NO_OP;
    }
}
