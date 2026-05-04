package fi.monopoly.presentation.game.desktop.assembly;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.desktop.LocalSessionActions;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.host.bot.GameBotTurnControlCoordinator;
import fi.monopoly.host.bot.GameBotTurnDriver;
import fi.monopoly.presentation.game.desktop.ui.GameDesktopControlsFactory;
import fi.monopoly.presentation.game.desktop.ui.GameDesktopPresentationHost;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.utils.DebugPerformanceStats;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Builds the desktop gameplay bootstrap bundle that {@code Game} still exposes as the local host.
 *
 * <p>This keeps the large constructor-time assembly step out of {@code Game} itself so the class
 * can keep its current compatibility surface while delegating more composition work into explicit
 * desktop assembly code.</p>
 */
@RequiredArgsConstructor
public final class GameDesktopBootstrapFactory {
    private final GameDesktopControlsFactory gameDesktopControlsFactory;
    private final GameDesktopHostFactory gameDesktopHostFactory;

    public GameDesktopBootstrapFactory() {
        this(new GameDesktopControlsFactory(), new GameDesktopHostFactory());
    }

    public GameDesktopBootstrap create(Config config, GameDesktopHostFactory.Hooks hooks) {
        GameDesktopControlsFactory.GameDesktopControls desktopControls = gameDesktopControlsFactory.create(config.runtime());
        GameDesktopHostFactory.GameDesktopHostContext hostContext = gameDesktopHostFactory.create(
                new GameDesktopHostFactory.Config(
                        config.runtime(),
                        config.restoredSessionState(),
                        config.localSessionActions(),
                        config.sessionId(),
                        config.supportedLocales(),
                        config.turnEngine(),
                        config.gameSessionStateCoordinator(),
                        config.gameBotTurnControlCoordinator(),
                        config.botTurnDriver(),
                        config.botTurnScheduler(),
                        config.debugPerformanceStats(),
                        desktopControls
                ),
                hooks
        );
        GameDesktopPresentationHost presentationHost = new GameDesktopPresentationHost(
                hostContext.presentationCoordinator(),
                hostContext.shellDependencies(),
                config.debugPerformanceStats(),
                config.sessionStateSupplier(),
                hooks.turnPlayerNameSupplier(),
                hostContext.gamePrimaryTurnControls(),
                hostContext.gameSessionQueries(),
                hostContext.gameUiController(),
                hostContext.gameFrameCoordinator()
        );
        return new GameDesktopBootstrap(desktopControls, hostContext, presentationHost);
    }

    public record Config(
            MonopolyRuntime runtime,
            SessionState restoredSessionState,
            LocalSessionActions localSessionActions,
            String sessionId,
            List<Locale> supportedLocales,
            fi.monopoly.components.turn.TurnEngine turnEngine,
            GameSessionStateCoordinator gameSessionStateCoordinator,
            GameBotTurnControlCoordinator gameBotTurnControlCoordinator,
            GameBotTurnDriver botTurnDriver,
            BotTurnScheduler botTurnScheduler,
            DebugPerformanceStats debugPerformanceStats,
            Supplier<GameSessionState> sessionStateSupplier
    ) {
    }

    public record GameDesktopBootstrap(
            GameDesktopControlsFactory.GameDesktopControls desktopControls,
            GameDesktopHostFactory.GameDesktopHostContext hostContext,
            GameDesktopPresentationHost presentationHost
    ) {
    }
}
