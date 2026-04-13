package fi.monopoly.presentation.game;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerDecision;
import fi.monopoly.components.computer.ComputerStrategies;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SessionState;

import java.util.function.LongConsumer;

public final class GameBotTurnDriver {
    private final BotTurnScheduler botTurnScheduler;

    public GameBotTurnDriver(BotTurnScheduler botTurnScheduler) {
        this.botTurnScheduler = botTurnScheduler;
    }

    public void step(Hooks hooks) {
        long stepStart = System.nanoTime();
        hooks.updateLogTurnContext();
        if (hooks.gameOver() || hooks.animationsRunning() || hooks.paused()) {
            return;
        }
        int now = hooks.now();
        if (botTurnScheduler.isWaiting(now)) {
            return;
        }
        hooks.syncPresentationState();
        SessionState sessionState = hooks.sessionState();
        if (sessionState.tradeState() != null) {
            handleTradeStep(hooks, sessionState, now, stepStart);
            return;
        }
        if (sessionState.auctionState() != null) {
            handleAuctionStep(hooks, sessionState, now, stepStart);
            return;
        }

        Player turnPlayer = hooks.currentTurnPlayer();
        if (turnPlayer == null || !turnPlayer.isComputerControlled()) {
            return;
        }

        ComputerTurnContext context = hooks.createTurnContext(turnPlayer);
        boolean acted = ComputerStrategies.forProfile(turnPlayer.getComputerProfile()).takeStep(context);
        if (acted) {
            hooks.scheduleNextAction(hooks.delayKindFor(context), now);
        }
        hooks.recordStep(System.nanoTime() - stepStart);
    }

    private void handleTradeStep(Hooks hooks, SessionState sessionState, int now, long stepStart) {
        Player tradeActor = hooks.findPlayerById(hooks.resolveTradeActorId(sessionState));
        if (tradeActor == null) {
            return;
        }
        if (!tradeActor.isComputerControlled()) {
            hooks.recordStep(System.nanoTime() - stepStart);
            return;
        }
        boolean acted = hooks.handleComputerTradeTurn(tradeActor);
        if (acted) {
            hooks.scheduleNextAction(BotTurnScheduler.DelayKind.TRADE, now);
        }
        hooks.recordStep(System.nanoTime() - stepStart);
    }

    private void handleAuctionStep(Hooks hooks, SessionState sessionState, int now, long stepStart) {
        if (sessionState.auctionState().status() == AuctionStatus.WON_PENDING_RESOLUTION
                && !hooks.popupVisible()) {
            boolean resolved = hooks.finishAuctionResolution(new FinishAuctionResolutionCommand(
                    hooks.sessionId(),
                    sessionState.auctionState().auctionId()
            ));
            if (resolved) {
                hooks.scheduleNextAction(BotTurnScheduler.DelayKind.AUCTION_ACTION, now);
            }
            hooks.recordStep(System.nanoTime() - stepStart);
            return;
        }
        Player turnPlayer = hooks.currentTurnPlayer();
        if (hooks.popupVisible()) {
            if (turnPlayer != null && turnPlayer.isComputerControlled() && hooks.resolveVisiblePopupFor(turnPlayer)) {
                hooks.scheduleNextAction(BotTurnScheduler.DelayKind.RESOLVE_POPUP, now);
            }
            hooks.recordStep(System.nanoTime() - stepStart);
            return;
        }
        Player auctionActor = hooks.findPlayerById(sessionState.auctionState().currentActorPlayerId());
        if (auctionActor == null || !auctionActor.isComputerControlled() || sessionState.auctionState().status() != AuctionStatus.ACTIVE) {
            return;
        }
        if (hooks.handleComputerAuctionAction(sessionState.auctionState().currentActorPlayerId())) {
            hooks.scheduleNextAction(BotTurnScheduler.DelayKind.AUCTION_ACTION, now);
        }
        hooks.recordStep(System.nanoTime() - stepStart);
    }

    public interface Hooks {
        void updateLogTurnContext();

        boolean gameOver();

        boolean animationsRunning();

        boolean paused();

        int now();

        void syncPresentationState();

        SessionState sessionState();

        Player currentTurnPlayer();

        Player findPlayerById(String playerId);

        String resolveTradeActorId(SessionState sessionState);

        boolean handleComputerTradeTurn(Player tradeActor);

        boolean popupVisible();

        boolean finishAuctionResolution(FinishAuctionResolutionCommand command);

        boolean resolveVisiblePopupFor(Player turnPlayer);

        boolean handleComputerAuctionAction(String actorPlayerId);

        ComputerTurnContext createTurnContext(Player turnPlayer);

        BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context);

        void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now);

        void recordStep(long durationNanos);

        String sessionId();
    }
}
