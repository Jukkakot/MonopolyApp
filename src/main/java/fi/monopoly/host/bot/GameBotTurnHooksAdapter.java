package fi.monopoly.host.bot;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.utils.DebugPerformanceStats;
import lombok.RequiredArgsConstructor;

import java.util.function.*;

@RequiredArgsConstructor
public final class GameBotTurnHooksAdapter implements GameBotTurnDriver.Hooks {
    private final SessionCommandPort sessionCommandPort;
    private final Function<String, CommandResult> computerAuctionActionHandler;
    private final BotSessionQueries sessionQueries;
    private final HostBotInteractionAdapter interactionAdapter;
    private final DebugPerformanceStats debugPerformanceStats;
    private final Runnable updateLogTurnContextAction;
    private final Runnable syncPresentationStateAction;
    private final BooleanSupplier gameOverSupplier;
    private final BooleanSupplier animationsRunningSupplier;
    private final BooleanSupplier pausedSupplier;
    private final IntSupplier nowSupplier;
    private final Consumer<BotTurnScheduler.DelayKind> scheduleNextActionConsumer;
    private final Supplier<String> sessionIdSupplier;
    private final BooleanSupplier recoverPrimaryTurnControlsSupplier;

    @Override
    public void updateLogTurnContext() {
        updateLogTurnContextAction.run();
    }

    @Override
    public boolean gameOver() {
        return gameOverSupplier.getAsBoolean();
    }

    @Override
    public boolean animationsRunning() {
        return animationsRunningSupplier.getAsBoolean();
    }

    @Override
    public boolean paused() {
        return pausedSupplier.getAsBoolean();
    }

    @Override
    public int now() {
        return nowSupplier.getAsInt();
    }

    @Override
    public void syncPresentationState() {
        syncPresentationStateAction.run();
    }

    @Override
    public SessionState sessionState() {
        return sessionCommandPort.currentState();
    }

    @Override
    public boolean isComputerPlayer(String playerId) {
        return sessionQueries.isComputerPlayer(playerId);
    }

    @Override
    public ComputerPlayerProfile computerProfileFor(String playerId) {
        return sessionQueries.computerProfileFor(playerId);
    }

    @Override
    public String resolveTradeActorId(SessionState sessionState) {
        return sessionQueries.resolveTradeActorId(sessionState);
    }

    @Override
    public boolean handleComputerTradeTurn(String actorId, ComputerPlayerProfile profile) {
        return interactionAdapter.handleComputerTradeTurn(actorId, profile);
    }

    @Override
    public boolean popupVisible() {
        return interactionAdapter.popupVisible();
    }

    @Override
    public boolean finishAuctionResolution(FinishAuctionResolutionCommand command) {
        return sessionCommandPort.handle(command).accepted();
    }

    @Override
    public boolean resolveVisiblePopupFor(ComputerPlayerProfile profile) {
        return interactionAdapter.resolveVisiblePopupFor(profile);
    }

    @Override
    public boolean handleComputerAuctionAction(String actorPlayerId) {
        return computerAuctionActionHandler.apply(actorPlayerId).accepted();
    }

    @Override
    public ComputerTurnContext createTurnContext(String playerId, ComputerPlayerProfile profile) {
        return new SessionBackedComputerTurnContext(
                playerId,
                profile,
                sessionCommandPort,
                interactionAdapter,
                syncPresentationStateAction
        );
    }

    @Override
    public BotTurnScheduler.DelayKind delayKindFor(ComputerTurnContext context) {
        return ((SessionBackedComputerTurnContext) context).delayKind();
    }

    @Override
    public void scheduleNextAction(BotTurnScheduler.DelayKind delayKind, int now) {
        scheduleNextActionConsumer.accept(delayKind);
    }

    @Override
    public void recordStep(long durationNanos) {
        debugPerformanceStats.recordComputerStep(durationNanos);
    }

    @Override
    public String sessionId() {
        return sessionIdSupplier.get();
    }

    @Override
    public boolean recoverPrimaryTurnControlsForCurrentComputerTurn() {
        return recoverPrimaryTurnControlsSupplier.getAsBoolean();
    }
}
