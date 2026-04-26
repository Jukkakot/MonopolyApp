package fi.monopoly.host.session.local;

import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Game;
import fi.monopoly.domain.session.SessionState;

import java.util.List;

/**
 * Transitional hosted-game adapter that exposes the current {@link Game} composition root through
 * the narrower desktop host interface.
 *
 * <p>This keeps embedded session hosting from depending directly on the concrete {@code Game}
 * class even while the runtime is still built around it. The adapter is intentionally thin: all
 * authoritative behavior still lives in the wrapped game instance.</p>
 */
public final class GameBackedDesktopHostedGame implements DesktopHostedGame {
    private final Game game;
    private final DesktopHostedGameView hostedGameView = new HostedGameView();

    public GameBackedDesktopHostedGame(Game game) {
        this.game = game;
    }

    public Game game() {
        return game;
    }

    @Override
    public SessionState sessionStateForPersistence() {
        return game.sessionStateForPersistence();
    }

    @Override
    public CommandResult submitCommand(SessionCommand command) {
        return game.submitCommand(command);
    }

    @Override
    public void showPersistenceNotice(String notice) {
        game.showPersistenceNotice(notice);
    }

    @Override
    public void advanceHostedFrame() {
        game.advanceHostedFrame();
    }

    @Override
    public DesktopHostedGameView view() {
        return hostedGameView;
    }

    @Override
    public void dispose() {
        game.dispose();
    }

    @Override
    public void setExternalCommandDelegate(SessionCommandPort delegate) {
        game.setExternalCommandDelegate(delegate);
    }

    private final class HostedGameView implements DesktopHostedGameView {
        @Override
        public void draw() {
            game.draw();
        }

        @Override
        public List<String> debugPerformanceLines(float fps) {
            return game.debugPerformanceLines(fps);
        }
    }
}
