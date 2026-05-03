package fi.monopoly.presentation.game.session;

import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.components.Player;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionStateCoordinatorTest {

    private final GameSessionStateCoordinator coordinator = new GameSessionStateCoordinator();

    @Test
    void togglePauseFlipsPausedStateAndRefreshesLabels() {
        GameSessionState sessionState = new GameSessionState();
        AtomicInteger refreshCount = new AtomicInteger();

        boolean changed = coordinator.togglePause(sessionState, refreshCount::incrementAndGet);

        assertTrue(changed);
        assertTrue(sessionState.paused());
        assertEquals(1, refreshCount.get());
    }

    @Test
    void togglePauseDoesNothingAfterGameOver() {
        GameSessionState sessionState = new GameSessionState();
        sessionState.setGameOver(true);
        AtomicInteger refreshCount = new AtomicInteger();

        boolean changed = coordinator.togglePause(sessionState, refreshCount::incrementAndGet);

        assertFalse(changed);
        assertFalse(sessionState.paused());
        assertEquals(0, refreshCount.get());
    }

    @Test
    void cycleBotSpeedModeAdvancesStateMarksSchedulerAndRefreshesLabels() {
        GameSessionState sessionState = new GameSessionState();
        AtomicInteger markReadyNow = new AtomicInteger(Integer.MIN_VALUE);
        AtomicInteger refreshCount = new AtomicInteger();

        BotTurnScheduler.SpeedMode nextMode = coordinator.cycleBotSpeedMode(
                sessionState,
                markReadyNow::set,
                1234,
                refreshCount::incrementAndGet
        );

        assertEquals(BotTurnScheduler.SpeedMode.FAST, nextMode);
        assertEquals(BotTurnScheduler.SpeedMode.FAST, sessionState.botSpeedMode());
        assertEquals(1234, markReadyNow.get());
        assertEquals(1, refreshCount.get());
    }

    @Test
    void persistenceNoticeExpiresAfterTimeout() {
        GameSessionState sessionState = new GameSessionState();

        coordinator.showPersistenceNotice(sessionState, "Saved", 100);
        assertEquals("Saved", sessionState.persistenceNotice());

        coordinator.clearExpiredPersistenceNoticeIfNeeded(sessionState, 5099);
        assertEquals("Saved", sessionState.persistenceNotice());

        coordinator.clearExpiredPersistenceNoticeIfNeeded(sessionState, 5100);
        assertNull(sessionState.persistenceNotice());
    }

    @Test
    void declareWinnerUpdatesStateAndInvokesHooks() {
        GameSessionState sessionState = new GameSessionState();
        sessionState.setPaused(true);
        Player winner = new Player("Winner", Color.PINK, 2000, 1);
        AtomicInteger resetTransientCalls = new AtomicInteger();
        AtomicInteger clearDebtCalls = new AtomicInteger();
        AtomicInteger updateDebtCalls = new AtomicInteger();
        AtomicInteger hideControlsCalls = new AtomicInteger();
        AtomicInteger refreshCalls = new AtomicInteger();
        AtomicInteger updateLogCalls = new AtomicInteger();
        AtomicReference<String> focusedWinnerId = new AtomicReference<>();
        AtomicReference<String> popupWinnerName = new AtomicReference<>();

        coordinator.declareWinner(sessionState, winner, new GameSessionStateCoordinator.WinnerHooks() {
            @Override
            public void resetTransientTurnState() {
                resetTransientCalls.incrementAndGet();
            }

            @Override
            public void clearDebtState() {
                clearDebtCalls.incrementAndGet();
            }

            @Override
            public void updateDebtButtons() {
                updateDebtCalls.incrementAndGet();
            }

            @Override
            public void hidePrimaryTurnControls() {
                hideControlsCalls.incrementAndGet();
            }

            @Override
            public void refreshLabels() {
                refreshCalls.incrementAndGet();
            }

            @Override
            public void focusWinner(String winnerPlayerId) {
                focusedWinnerId.set(winnerPlayerId);
            }

            @Override
            public void updateLogTurnContext() {
                updateLogCalls.incrementAndGet();
            }

            @Override
            public void showVictoryPopup(String winnerName) {
                popupWinnerName.set(winnerName);
            }
        });

        assertTrue(sessionState.gameOver());
        assertFalse(sessionState.paused());
        assertSame(winner, sessionState.winner());
        assertEquals(1, resetTransientCalls.get());
        assertEquals(1, clearDebtCalls.get());
        assertEquals(1, updateDebtCalls.get());
        assertEquals(1, hideControlsCalls.get());
        assertEquals(1, refreshCalls.get());
        assertEquals(1, updateLogCalls.get());
        assertEquals(sessionState.winnerPlayerId(), focusedWinnerId.get());
        assertEquals(winner.getName(), popupWinnerName.get());
    }

    @Test
    void debugResetTurnStateRunsAllUiRecoverySteps() {
        AtomicBoolean animationsFinished = new AtomicBoolean();
        AtomicBoolean transientTurnReset = new AtomicBoolean();
        AtomicBoolean debtCleared = new AtomicBoolean();
        AtomicBoolean debtButtonsUpdated = new AtomicBoolean();
        AtomicBoolean popupsHidden = new AtomicBoolean();
        AtomicBoolean rollShown = new AtomicBoolean();
        AtomicBoolean messageShown = new AtomicBoolean();

        coordinator.debugResetTurnState(new GameSessionStateCoordinator.DebugResetHooks() {
            @Override
            public void finishAllAnimations() {
                animationsFinished.set(true);
            }

            @Override
            public void resetTransientTurnState() {
                transientTurnReset.set(true);
            }

            @Override
            public void clearDebtState() {
                debtCleared.set(true);
            }

            @Override
            public void updateDebtButtons() {
                debtButtonsUpdated.set(true);
            }

            @Override
            public void hideAllPopups() {
                popupsHidden.set(true);
            }

            @Override
            public void showRollDiceControl() {
                rollShown.set(true);
            }

            @Override
            public void showDebugResetMessage() {
                messageShown.set(true);
            }
        });

        assertTrue(animationsFinished.get());
        assertTrue(transientTurnReset.get());
        assertTrue(debtCleared.get());
        assertTrue(debtButtonsUpdated.get());
        assertTrue(popupsHidden.get());
        assertTrue(rollShown.get());
        assertTrue(messageShown.get());
    }
}
