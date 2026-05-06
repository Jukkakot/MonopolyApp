package fi.monopoly.host.bot;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.components.computer.DebtView;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.components.computer.VisibleActionsView;
import fi.monopoly.domain.session.DebtAction;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBotTurnDriverTest {

    @Test
    void debtStepUsesDebtorEvenWhenTurnPointerStillTargetsAnotherPlayer() {
        GameBotTurnDriver driver = new GameBotTurnDriver(new BotTurnScheduler(() -> false));
        Player debtor = new Player("Debtor", Color.PINK, 1500, 2, ComputerPlayerProfile.SMOKE_TEST);
        String debtorPlayerId = "player-" + debtor.getId();
        HooksStub hooks = new HooksStub(debtorPlayerId, ComputerPlayerProfile.SMOKE_TEST, debtSessionState(debtor));

        driver.step(hooks);

        assertEquals(debtorPlayerId, hooks.contextPlayerId);
        assertTrue(hooks.scheduled);
        assertEquals(BotTurnScheduler.DelayKind.RETRY_DEBT_PAYMENT, hooks.scheduledDelayKind);
        assertFalse(hooks.recoverPrimaryTurnControlsCalled);
    }

    private static SessionState debtSessionState(Player debtor) {
        return new SessionState(
                "session-1",
                1L,
                SessionStatus.IN_PROGRESS,
                List.of(),
                List.of(),
                List.of(),
                new TurnState("player-999", TurnPhase.RESOLVING_DEBT, false, false),
                null,
                null,
                new DebtStateModel(
                        "debt-1",
                        "player-" + debtor.getId(),
                        DebtCreditorType.BANK,
                        null,
                        100,
                        "Rent",
                        false,
                        100,
                        0,
                        List.of(DebtAction.PAY_DEBT_NOW)
                ),
                null,
                null
        );
    }

    private static final class HooksStub implements GameBotTurnDriver.Hooks {
        private final String computerPlayerId;
        private final ComputerPlayerProfile computerProfile;
        private final SessionState sessionState;
        String contextPlayerId;
        boolean scheduled;
        BotTurnScheduler.DelayKind scheduledDelayKind;
        boolean recoverPrimaryTurnControlsCalled;

        private HooksStub(String computerPlayerId, ComputerPlayerProfile computerProfile, SessionState sessionState) {
            this.computerPlayerId = computerPlayerId;
            this.computerProfile = computerProfile;
            this.sessionState = sessionState;
        }

        @Override
        public void updateLogTurnContext() {
        }

        @Override
        public boolean gameOver() {
            return false;
        }

        @Override
        public boolean animationsRunning() {
            return false;
        }

        @Override
        public boolean paused() {
            return false;
        }

        @Override
        public int now() {
            return 0;
        }

        @Override
        public void syncPresentationState() {
        }

        @Override
        public SessionState sessionState() {
            return sessionState;
        }

        @Override
        public boolean isComputerPlayer(String playerId) {
            return computerPlayerId.equals(playerId);
        }

        @Override
        public ComputerPlayerProfile computerProfileFor(String playerId) {
            return computerPlayerId.equals(playerId) ? computerProfile : ComputerPlayerProfile.HUMAN;
        }

        @Override
        public String resolveTradeActorId(SessionState sessionState) {
            return null;
        }

        @Override
        public boolean handleComputerTradeTurn(String actorId, ComputerPlayerProfile profile) {
            return false;
        }

        @Override
        public boolean popupVisible() {
            return false;
        }

        @Override
        public boolean finishAuctionResolution(fi.monopoly.application.command.FinishAuctionResolutionCommand command) {
            return false;
        }

        @Override
        public boolean resolveVisiblePopupFor(ComputerPlayerProfile profile) {
            return false;
        }

        @Override
        public boolean handleComputerAuctionAction(String actorPlayerId) {
            return false;
        }

        @Override
        public ComputerTurnContext createTurnContext(String playerId, ComputerPlayerProfile profile) {
            contextPlayerId = playerId;
            return new ComputerTurnContext() {
                @Override
                public GameView gameView() {
                    return new GameView(
                            0,
                            List.of(currentPlayerView()),
                            new VisibleActionsView(false, true, false, false, false),
                            null,
                            new DebtView(100, "Rent", false, "BANK", "Bank"),
                            0,
                            0,
                            0
                    );
                }

                @Override
                public PlayerView currentPlayerView() {
                    return new PlayerView(
                            0,
                            "Debtor",
                            1500,
                            2,
                            computerProfile,
                            null,
                            false,
                            0,
                            0,
                            0,
                            0,
                            0,
                            0,
                            List.of(),
                            List.of()
                    );
                }

                @Override
                public SessionState sessionState() {
                    return sessionState;
                }

                @Override
                public boolean submit(SessionCommand command) {
                    return true;
                }

                @Override
                public boolean resolveActivePopup() {
                    return false;
                }

                @Override
                public boolean acceptActivePopup() {
                    return false;
                }

                @Override
                public boolean declineActivePopup() {
                    return false;
                }

                @Override
                public ComputerDecision initiateTrade() {
                    return null;
                }
            };
        }

        @Override
        public BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context) {
            return BotTurnScheduler.DelayKind.RETRY_DEBT_PAYMENT;
        }

        @Override
        public void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now) {
            scheduled = true;
            scheduledDelayKind = delayKind;
        }

        @Override
        public void recordStep(long durationNanos) {
        }

        @Override
        public String sessionId() {
            return sessionState.sessionId();
        }

        @Override
        public boolean recoverPrimaryTurnControlsForCurrentComputerTurn() {
            recoverPrimaryTurnControlsCalled = true;
            return false;
        }
    }
}
