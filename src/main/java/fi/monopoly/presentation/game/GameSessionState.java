package fi.monopoly.presentation.game;

import fi.monopoly.components.Player;

/**
 * Mutable desktop session state that still belongs to the local game shell.
 *
 * <p>The application session already owns authoritative turn/debt/trade data, but the desktop
 * shell also needs transient presentation-facing state such as pause mode, winner banner state,
 * bot speed, and short-lived persistence notices. Keeping these fields together makes it easier to
 * move local session hosting behind a backend boundary later.</p>
 */
public final class GameSessionState {
    private boolean paused;
    private boolean gameOver;
    private Player winner;
    private BotTurnScheduler.SpeedMode botSpeedMode = BotTurnScheduler.SpeedMode.NORMAL;
    private String persistenceNotice;
    private int persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;

    public boolean paused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean gameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public Player winner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
    }

    public BotTurnScheduler.SpeedMode botSpeedMode() {
        return botSpeedMode;
    }

    public void setBotSpeedMode(BotTurnScheduler.SpeedMode botSpeedMode) {
        this.botSpeedMode = botSpeedMode;
    }

    public String persistenceNotice() {
        return persistenceNotice;
    }

    public int persistenceNoticeExpiresAtMillis() {
        return persistenceNoticeExpiresAtMillis;
    }

    public void clearPersistenceNotice() {
        persistenceNotice = null;
        persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;
    }

    public void setPersistenceNotice(String persistenceNotice, int expiresAtMillis) {
        this.persistenceNotice = persistenceNotice;
        this.persistenceNoticeExpiresAtMillis = expiresAtMillis;
    }
}
