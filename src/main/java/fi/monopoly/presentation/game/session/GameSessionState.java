package fi.monopoly.presentation.game.session;

import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.components.Player;
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
    private Player winner;
    private String winnerPlayerId;
    private BotTurnScheduler.SpeedMode botSpeedMode = BotTurnScheduler.SpeedMode.NORMAL;
    private String persistenceNotice;
    private int persistenceNoticeExpiresAtMillis = Integer.MIN_VALUE;

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
        this.winnerPlayerId = winner != null ? "player-" + winner.getId() : null;
    }

    public void setWinnerPlayerId(String winnerPlayerId) {
        this.winnerPlayerId = winnerPlayerId;
    }

    public String winnerName() {
        return winner != null ? winner.getName() : null;
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
