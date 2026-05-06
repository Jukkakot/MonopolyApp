package fi.monopoly.host.bot;

import fi.monopoly.types.DiceState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class GameBotTurnControlCoordinatorTest {
    private final GameBotTurnControlCoordinator coordinator = new GameBotTurnControlCoordinator();

    @Test
    void projectedActionIsNoneWhenPopupBlocksBotTurn() {
        Hooks hooks = new Hooks();
        hooks.popupVisible = true;

        assertEquals(GameBotTurnControlCoordinator.BotPrimaryAction.NONE, coordinator.projectedAction(hooks, true, true));
    }

    @Test
    void projectedActionRequestsRollWhenNoDiceResultExists() {
        assertEquals(GameBotTurnControlCoordinator.BotPrimaryAction.ROLL_DICE, coordinator.projectedAction(new Hooks(), true, true));
    }

    @Test
    void projectedActionRequestsRollAfterDoubles() {
        Hooks hooks = new Hooks();
        hooks.currentDiceState = DiceState.DOUBLES;

        assertEquals(GameBotTurnControlCoordinator.BotPrimaryAction.ROLL_DICE, coordinator.projectedAction(hooks, true, true));
    }

    @Test
    void projectedActionRequestsEndTurnAfterNormalRoll() {
        Hooks hooks = new Hooks();
        hooks.currentDiceState = DiceState.NOREROLL;

        assertEquals(GameBotTurnControlCoordinator.BotPrimaryAction.END_TURN, coordinator.projectedAction(hooks, true, true));
    }

    @Test
    void restoreControlsShowsRollDiceWhenBotNeedsNewRoll() {
        Hooks hooks = new Hooks();

        assertTrue(coordinator.restoreControlsIfNeeded(hooks, true, true));
        assertTrue(hooks.rollShown.get());
        assertFalse(hooks.endShown.get());
    }

    @Test
    void restoreControlsShowsEndTurnAfterNormalRoll() {
        Hooks hooks = new Hooks();
        hooks.currentDiceState = DiceState.NOREROLL;

        assertTrue(coordinator.restoreControlsIfNeeded(hooks, true, true));
        assertFalse(hooks.rollShown.get());
        assertTrue(hooks.endShown.get());
    }

    @Test
    void restoreControlsDoesNothingWhenControlAlreadyAvailable() {
        Hooks hooks = new Hooks();
        hooks.rollDiceAvailable = true;

        assertFalse(coordinator.restoreControlsIfNeeded(hooks, true, true));
        assertFalse(hooks.rollShown.get());
        assertFalse(hooks.endShown.get());
    }

    @Test
    void restoreControlsDoesNothingForHumanTurn() {
        Hooks hooks = new Hooks();

        assertFalse(coordinator.restoreControlsIfNeeded(hooks, true, false));
    }

    private static final class Hooks implements GameBotTurnControlCoordinator.Hooks {
        private boolean gameOver;
        private boolean popupVisible;
        private boolean debtActive;
        private boolean animationsRunning;
        private boolean activeAuctionOpen;
        private boolean activeTradeOpen;
        private boolean auctionOverrideActive;
        private boolean tradeOverrideActive;
        private boolean pendingDecisionOverrideActive;
        private DiceState currentDiceState;
        private boolean rollDiceAvailable;
        private boolean endTurnAvailable;
        private final AtomicBoolean rollShown = new AtomicBoolean();
        private final AtomicBoolean endShown = new AtomicBoolean();

        @Override
        public boolean gameOver() {
            return gameOver;
        }

        @Override
        public boolean popupVisible() {
            return popupVisible;
        }

        @Override
        public boolean debtActive() {
            return debtActive;
        }

        @Override
        public boolean animationsRunning() {
            return animationsRunning;
        }

        @Override
        public boolean activeAuctionOpen() {
            return activeAuctionOpen;
        }

        @Override
        public boolean activeTradeOpen() {
            return activeTradeOpen;
        }

        @Override
        public boolean auctionOverrideActive() {
            return auctionOverrideActive;
        }

        @Override
        public boolean tradeOverrideActive() {
            return tradeOverrideActive;
        }

        @Override
        public boolean pendingDecisionOverrideActive() {
            return pendingDecisionOverrideActive;
        }

        @Override
        public DiceState currentDiceState() {
            return currentDiceState;
        }

        @Override
        public boolean rollDiceActionAlreadyAvailable() {
            return rollDiceAvailable;
        }

        @Override
        public boolean endTurnActionAlreadyAvailable() {
            return endTurnAvailable;
        }

        @Override
        public void showRollDiceControl() {
            rollShown.set(true);
        }

        @Override
        public void showEndTurnControl() {
            endShown.set(true);
        }
    }
}
