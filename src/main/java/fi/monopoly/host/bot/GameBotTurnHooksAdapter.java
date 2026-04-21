package fi.monopoly.host.bot;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerTurnContext;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.utils.DebugPerformanceStats;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class GameBotTurnHooksAdapter implements GameBotTurnDriver.Hooks {
    private final SessionApplicationService sessionApplicationService;
    private final GameSessionQueries gameSessionQueries;
    private final HostBotInteractionAdapter interactionAdapter;
    private final DebugPerformanceStats debugPerformanceStats;
    private final Supplier<Player> turnPlayerSupplier;
    private final Runnable updateLogTurnContextAction;
    private final Runnable syncPresentationStateAction;
    private final BooleanSupplier gameOverSupplier;
    private final BooleanSupplier animationsRunningSupplier;
    private final BooleanSupplier pausedSupplier;
    private final IntSupplier nowSupplier;
    private final Consumer<BotTurnScheduler.DelayKind> scheduleNextActionConsumer;
    private final Supplier<String> sessionIdSupplier;
    private final BooleanSupplier projectedRollDiceAvailableSupplier;
    private final BooleanSupplier projectedEndTurnAvailableSupplier;
    private final BooleanSupplier recoverPrimaryTurnControlsSupplier;

    public GameBotTurnHooksAdapter(
            SessionApplicationService sessionApplicationService,
            GameSessionQueries gameSessionQueries,
            HostBotInteractionAdapter interactionAdapter,
            DebugPerformanceStats debugPerformanceStats,
            Supplier<Player> turnPlayerSupplier,
            Runnable updateLogTurnContextAction,
            Runnable syncPresentationStateAction,
            BooleanSupplier gameOverSupplier,
            BooleanSupplier animationsRunningSupplier,
            BooleanSupplier pausedSupplier,
            IntSupplier nowSupplier,
            Consumer<BotTurnScheduler.DelayKind> scheduleNextActionConsumer,
            Supplier<String> sessionIdSupplier,
            BooleanSupplier projectedRollDiceAvailableSupplier,
            BooleanSupplier projectedEndTurnAvailableSupplier,
            BooleanSupplier recoverPrimaryTurnControlsSupplier
    ) {
        this.sessionApplicationService = sessionApplicationService;
        this.gameSessionQueries = gameSessionQueries;
        this.interactionAdapter = interactionAdapter;
        this.debugPerformanceStats = debugPerformanceStats;
        this.turnPlayerSupplier = turnPlayerSupplier;
        this.updateLogTurnContextAction = updateLogTurnContextAction;
        this.syncPresentationStateAction = syncPresentationStateAction;
        this.gameOverSupplier = gameOverSupplier;
        this.animationsRunningSupplier = animationsRunningSupplier;
        this.pausedSupplier = pausedSupplier;
        this.nowSupplier = nowSupplier;
        this.scheduleNextActionConsumer = scheduleNextActionConsumer;
        this.sessionIdSupplier = sessionIdSupplier;
        this.projectedRollDiceAvailableSupplier = projectedRollDiceAvailableSupplier;
        this.projectedEndTurnAvailableSupplier = projectedEndTurnAvailableSupplier;
        this.recoverPrimaryTurnControlsSupplier = recoverPrimaryTurnControlsSupplier;
    }

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
        return sessionApplicationService.currentState();
    }

    @Override
    public Player currentTurnPlayer() {
        return turnPlayerSupplier.get();
    }

    @Override
    public Player findPlayerById(String playerId) {
        return gameSessionQueries.findPlayerById(playerId);
    }

    @Override
    public String resolveTradeActorId(SessionState sessionState) {
        return gameSessionQueries.resolveTradeActorId(sessionState);
    }

    @Override
    public boolean handleComputerTradeTurn(Player tradeActor) {
        return interactionAdapter.handleComputerTradeTurn(tradeActor);
    }

    @Override
    public boolean popupVisible() {
        return interactionAdapter.popupVisible();
    }

    @Override
    public boolean finishAuctionResolution(FinishAuctionResolutionCommand command) {
        return sessionApplicationService.handle(command).accepted();
    }

    @Override
    public boolean resolveVisiblePopupFor(Player turnPlayer) {
        return interactionAdapter.resolveVisiblePopupFor(turnPlayer);
    }

    @Override
    public boolean handleComputerAuctionAction(String actorPlayerId) {
        return sessionApplicationService.handleComputerAuctionAction(actorPlayerId).accepted();
    }

    @Override
    public ComputerTurnContext createTurnContext(Player turnPlayer) {
        return new SessionBackedComputerTurnContext(
                turnPlayer,
                sessionApplicationService,
                interactionAdapter,
                syncPresentationStateAction,
                projectedRollDiceAvailableSupplier::getAsBoolean,
                projectedEndTurnAvailableSupplier::getAsBoolean
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
