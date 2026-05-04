package fi.monopoly.host.bot;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerStrategies;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SessionState;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GameBotTurnDriver {
    private final BotTurnScheduler botTurnScheduler;

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

        Player actingPlayer = resolveActingPlayer(hooks, sessionState);
        if (actingPlayer == null || !actingPlayer.isComputerControlled()) {
            return;
        }

        ComputerTurnContext context = hooks.createTurnContext(actingPlayer);
        boolean acted = ComputerStrategies.forProfile(actingPlayer.getComputerProfile()).takeStep(context);
        if (!acted && sessionState.activeDebt() == null && hooks.recoverPrimaryTurnControlsForCurrentComputerTurn()) {
            hooks.syncPresentationState();
            acted = ComputerStrategies.forProfile(actingPlayer.getComputerProfile()).takeStep(context);
        }
        if (acted) {
            hooks.scheduleNextAction(hooks.delayKindFor(context), now);
        }
        hooks.recordStep(System.nanoTime() - stepStart);
    }

    private Player resolveActingPlayer(Hooks hooks, SessionState sessionState) {
        if (sessionState.activeDebt() != null) {
            Player debtor = hooks.findPlayerById(sessionState.activeDebt().debtorPlayerId());
            if (debtor != null) {
                return debtor;
            }
        }
        if (sessionState.turn() == null || sessionState.turn().activePlayerId() == null) return null;
        return hooks.findPlayerById(sessionState.turn().activePlayerId());
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
        if (hooks.popupVisible()) {
            String activePlayerId = sessionState.turn() != null ? sessionState.turn().activePlayerId() : null;
            Player turnPlayer = activePlayerId != null ? hooks.findPlayerById(activePlayerId) : null;
            if (turnPlayer != null && turnPlayer.isComputerControlled() && hooks.resolveVisiblePopupFor(turnPlayer.getComputerProfile())) {
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

        Player findPlayerById(String playerId);

        String resolveTradeActorId(SessionState sessionState);

        boolean handleComputerTradeTurn(Player tradeActor);

        boolean popupVisible();

        boolean finishAuctionResolution(FinishAuctionResolutionCommand command);

        boolean resolveVisiblePopupFor(ComputerPlayerProfile profile);

        boolean handleComputerAuctionAction(String actorPlayerId);

        ComputerTurnContext createTurnContext(Player turnPlayer);

        BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context);

        void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now);

        void recordStep(long durationNanos);

        String sessionId();

        boolean recoverPrimaryTurnControlsForCurrentComputerTurn();
    }
}
