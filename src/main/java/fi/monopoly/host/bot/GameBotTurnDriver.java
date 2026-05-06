package fi.monopoly.host.bot;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
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

        String actingPlayerId = resolveActingPlayerId(sessionState);
        if (actingPlayerId == null || !hooks.isComputerPlayer(actingPlayerId)) {
            return;
        }

        ComputerPlayerProfile profile = hooks.computerProfileFor(actingPlayerId);
        ComputerTurnContext context = hooks.createTurnContext(actingPlayerId, profile);
        boolean acted = ComputerStrategies.forProfile(profile).takeStep(context);
        if (!acted && sessionState.activeDebt() == null && hooks.recoverPrimaryTurnControlsForCurrentComputerTurn()) {
            hooks.syncPresentationState();
            acted = ComputerStrategies.forProfile(profile).takeStep(context);
        }
        if (acted) {
            hooks.scheduleNextAction(hooks.delayKindFor(context), now);
        }
        hooks.recordStep(System.nanoTime() - stepStart);
    }

    private String resolveActingPlayerId(SessionState sessionState) {
        if (sessionState.activeDebt() != null) {
            return sessionState.activeDebt().debtorPlayerId();
        }
        if (sessionState.turn() == null) return null;
        return sessionState.turn().activePlayerId();
    }

    private void handleTradeStep(Hooks hooks, SessionState sessionState, int now, long stepStart) {
        String tradeActorId = hooks.resolveTradeActorId(sessionState);
        if (tradeActorId == null || !hooks.isComputerPlayer(tradeActorId)) {
            hooks.recordStep(System.nanoTime() - stepStart);
            return;
        }
        ComputerPlayerProfile profile = hooks.computerProfileFor(tradeActorId);
        boolean acted = hooks.handleComputerTradeTurn(tradeActorId, profile);
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
            if (activePlayerId != null && hooks.isComputerPlayer(activePlayerId)
                    && hooks.resolveVisiblePopupFor(hooks.computerProfileFor(activePlayerId))) {
                hooks.scheduleNextAction(BotTurnScheduler.DelayKind.RESOLVE_POPUP, now);
            }
            hooks.recordStep(System.nanoTime() - stepStart);
            return;
        }
        String auctionActorId = sessionState.auctionState().currentActorPlayerId();
        if (auctionActorId == null || !hooks.isComputerPlayer(auctionActorId)
                || sessionState.auctionState().status() != AuctionStatus.ACTIVE) {
            return;
        }
        if (hooks.handleComputerAuctionAction(auctionActorId)) {
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

        boolean isComputerPlayer(String playerId);

        ComputerPlayerProfile computerProfileFor(String playerId);

        String resolveTradeActorId(SessionState sessionState);

        boolean handleComputerTradeTurn(String actorId, ComputerPlayerProfile profile);

        boolean popupVisible();

        boolean finishAuctionResolution(FinishAuctionResolutionCommand command);

        boolean resolveVisiblePopupFor(ComputerPlayerProfile profile);

        boolean handleComputerAuctionAction(String actorPlayerId);

        ComputerTurnContext createTurnContext(String playerId, ComputerPlayerProfile profile);

        BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context);

        void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now);

        void recordStep(long durationNanos);

        String sessionId();

        boolean recoverPrimaryTurnControlsForCurrentComputerTurn();
    }
}
