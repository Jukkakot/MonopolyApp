package fi.monopoly.presentation.game.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.presentation.game.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.desktop.RestoredSessionReattachmentCoordinator;
import fi.monopoly.presentation.session.debt.DebtController;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

/**
 * Coordinates desktop-only session state around the authoritative application session.
 *
 * <p>This keeps local shell concerns such as pause mode, restored presentation reattachment,
 * winner handling, bot speed, and persistence notices out of {@code Game} so the composition root
 * can gradually shrink toward a backend-hosted architecture.</p>
 */
public final class GameSessionStateCoordinator {
    private static final int PERSISTENCE_NOTICE_DURATION_MILLIS = 5000;

    private final RestoredSessionReattachmentCoordinator restoredSessionReattachmentCoordinator;

    public GameSessionStateCoordinator() {
        this(new RestoredSessionReattachmentCoordinator());
    }

    GameSessionStateCoordinator(RestoredSessionReattachmentCoordinator restoredSessionReattachmentCoordinator) {
        this.restoredSessionReattachmentCoordinator = restoredSessionReattachmentCoordinator;
    }

    public void restoreSessionState(
            GameSessionState sessionState,
            fi.monopoly.domain.session.SessionState restoredSessionState,
            SessionApplicationService sessionApplicationService,
            Function<String, Player> playerById
    ) {
        RestoredSessionReattachmentCoordinator.RestoredGameState restoredGameState =
                restoredSessionReattachmentCoordinator.restoreAuthoritativeState(
                        restoredSessionState,
                        sessionApplicationService,
                        playerById
                );
        sessionState.setPaused(restoredGameState.paused());
        sessionState.setGameOver(restoredGameState.gameOver());
        sessionState.setWinner(restoredGameState.winner());
    }

    public void initializePresentation(
            fi.monopoly.domain.session.SessionState restoredSessionState,
            SessionApplicationService sessionApplicationService,
            DebtController debtController,
            RestoredSessionReattachmentCoordinator.Hooks hooks
    ) {
        restoredSessionReattachmentCoordinator.restorePresentation(
                restoredSessionState,
                sessionApplicationService,
                debtController,
                hooks
        );
    }

    public void showPersistenceNotice(GameSessionState sessionState, String notice, int nowMillis) {
        if (notice == null || notice.isBlank()) {
            sessionState.clearPersistenceNotice();
            return;
        }
        sessionState.setPersistenceNotice(notice, nowMillis + PERSISTENCE_NOTICE_DURATION_MILLIS);
    }

    public void clearExpiredPersistenceNoticeIfNeeded(GameSessionState sessionState, int nowMillis) {
        if (sessionState.persistenceNotice() == null) {
            return;
        }
        if (nowMillis < sessionState.persistenceNoticeExpiresAtMillis()) {
            return;
        }
        sessionState.clearPersistenceNotice();
    }

    public void onDebtStateChanged(
            SessionApplicationService sessionApplicationService,
            Runnable updateDebtButtons,
            Runnable restoreBotTurnControlsIfNeeded
    ) {
        updateDebtButtons.run();
        if (sessionApplicationService != null) {
            sessionApplicationService.clearActiveDebtOverride();
        }
        restoreBotTurnControlsIfNeeded.run();
    }

    public boolean togglePause(GameSessionState sessionState, Runnable refreshLabels) {
        if (sessionState.gameOver()) {
            return false;
        }
        sessionState.setPaused(!sessionState.paused());
        refreshLabels.run();
        return true;
    }

    public BotTurnScheduler.SpeedMode cycleBotSpeedMode(
            GameSessionState sessionState,
            IntConsumer markBotReadyNow,
            int nowMillis,
            Runnable refreshLabels
    ) {
        BotTurnScheduler.SpeedMode nextMode = sessionState.botSpeedMode().next();
        sessionState.setBotSpeedMode(nextMode);
        markBotReadyNow.accept(nowMillis);
        refreshLabels.run();
        return nextMode;
    }

    public void debugResetTurnState(DebugResetHooks hooks) {
        hooks.finishAllAnimations();
        hooks.resetTransientTurnState();
        hooks.clearDebtState();
        hooks.updateDebtButtons();
        hooks.hideAllPopups();
        hooks.showRollDiceControl();
        hooks.showDebugResetMessage();
    }

    public void restoreNormalTurnControls(Runnable clearDebtState, Runnable showRollDiceControl) {
        clearDebtState.run();
        showRollDiceControl.run();
    }

    public void declareWinner(GameSessionState sessionState, Player winningPlayer, WinnerHooks hooks) {
        sessionState.setGameOver(true);
        sessionState.setWinner(winningPlayer);
        sessionState.setPaused(false);
        hooks.resetTransientTurnState();
        hooks.clearDebtState();
        hooks.updateDebtButtons();
        hooks.hidePrimaryTurnControls();
        hooks.refreshLabels();
        if (winningPlayer != null) {
            hooks.focusWinner(winningPlayer);
        }
        hooks.updateLogTurnContext();
        hooks.showVictoryPopup(winningPlayer);
    }

    public interface DebugResetHooks {
        void finishAllAnimations();

        void resetTransientTurnState();

        void clearDebtState();

        void updateDebtButtons();

        void hideAllPopups();

        void showRollDiceControl();

        void showDebugResetMessage();
    }

    public interface WinnerHooks {
        void resetTransientTurnState();

        void clearDebtState();

        void updateDebtButtons();

        void hidePrimaryTurnControls();

        void refreshLabels();

        void focusWinner(Player winner);

        void updateLogTurnContext();

        void showVictoryPopup(Player winner);
    }
}
