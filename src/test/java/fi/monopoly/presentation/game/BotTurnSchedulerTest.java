package fi.monopoly.presentation.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BotTurnSchedulerTest {

    @Test
    void instantModeNeverWaitsAfterScheduling() {
        BotTurnScheduler scheduler = new BotTurnScheduler();

        scheduler.schedule(BotTurnScheduler.DelayKind.END_TURN, 1000, BotTurnScheduler.SpeedMode.INSTANT, false);

        assertFalse(scheduler.isWaiting(1000));
    }

    @Test
    void markReadyNowClearsWaitingState() {
        BotTurnScheduler scheduler = new BotTurnScheduler();

        scheduler.schedule(BotTurnScheduler.DelayKind.END_TURN, 1000, BotTurnScheduler.SpeedMode.NORMAL, false);
        scheduler.markReadyNow(1000);

        assertFalse(scheduler.isWaiting(1000));
    }

    @Test
    void animationFinishCooldownOnlySchedulesForComputerTurns() {
        BotTurnScheduler scheduler = new BotTurnScheduler();

        scheduler.applyAnimationFinishCooldownIfNeeded(true, false, false, 1000, BotTurnScheduler.SpeedMode.NORMAL, false);

        assertFalse(scheduler.isWaiting(1000));
    }
}
