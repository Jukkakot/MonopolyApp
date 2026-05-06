package fi.monopoly.presentation.game.session;

import fi.monopoly.host.bot.BotTurnScheduler;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Mutable desktop session state that still belongs to the local game shell.
 *
 * <p>The application session already owns authoritative turn/debt/trade data, but the desktop
 * shell also needs transient presentation-facing state such as pause mode, winner banner state,
 * bot speed, and short-lived persistence notices. Keeping these fields together makes it easier to
 * move local session hosting behind a backend boundary later.</p>
 */
@Getter
@Accessors(fluent = true)
public final class GameSessionState {
    private boolean paused;
    private boolean gameOver;
    private String winnerPlayerId;
    private String winnerName;
    private BotTurnScheduler.SpeedMode botSpeedMode = BotTurnScheduler.SpeedMode.NORMAL;
    private String persistenceNotice;
    private int persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void setWinnerInfo(String winnerPlayerId, String winnerName) {
        this.winnerPlayerId = winnerPlayerId;
        this.winnerName = winnerName;
    }

    public String winnerName() {
        return winnerName;
    }

    public void setBotSpeedMode(BotTurnScheduler.SpeedMode botSpeedMode) {
        this.botSpeedMode = botSpeedMode;
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
