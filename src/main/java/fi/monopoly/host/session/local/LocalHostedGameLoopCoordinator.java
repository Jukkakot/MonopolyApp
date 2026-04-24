package fi.monopoly.host.session.local;

import fi.monopoly.host.bot.GameBotTurnDriver;
import fi.monopoly.utils.DebugPerformanceStats;

/**
 * Host-owned local game loop coordinator for embedded desktop sessions.
 *
 * <p>This keeps authoritative frame advancement order on the host side even in single-process
 * mode: advance presentation/runtime state first, then let host-owned bot stepping mutate the
 * session, and only after that record the frame as one host tick.</p>
 *
 * <p>Presentation frame advancement is injected as a {@link Runnable} so this coordinator has no
 * compile-time dependency on the desktop presentation layer.</p>
 */
public final class LocalHostedGameLoopCoordinator {
    private final Runnable presentationFrameAdvancer;
    private final GameBotTurnDriver gameBotTurnDriver;
    private final GameBotTurnDriver.Hooks botTurnHooks;
    private final DebugPerformanceStats debugPerformanceStats;

    public LocalHostedGameLoopCoordinator(
            Runnable presentationFrameAdvancer,
            GameBotTurnDriver gameBotTurnDriver,
            GameBotTurnDriver.Hooks botTurnHooks,
            DebugPerformanceStats debugPerformanceStats
    ) {
        this.presentationFrameAdvancer = presentationFrameAdvancer;
        this.gameBotTurnDriver = gameBotTurnDriver;
        this.botTurnHooks = botTurnHooks;
        this.debugPerformanceStats = debugPerformanceStats;
    }

    public void advanceFrame() {
        long frameStart = System.nanoTime();
        presentationFrameAdvancer.run();
        runBotStep();
        debugPerformanceStats.recordFrame(System.nanoTime() - frameStart);
    }

    public void runBotStep() {
        gameBotTurnDriver.step(botTurnHooks);
    }
}
