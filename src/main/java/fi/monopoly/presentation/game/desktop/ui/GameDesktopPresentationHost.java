package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.client.desktop.DesktopImageCatalog;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.GameView;
import fi.monopoly.components.computer.PlayerView;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.SessionViewFacade;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopPresentationCoordinator;
import fi.monopoly.presentation.game.desktop.shell.GameDesktopShellDependencies;
import fi.monopoly.presentation.game.session.GameSessionQueries;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import processing.event.Event;

import java.util.List;
import java.util.function.Supplier;

/**
 * Owns the desktop-side presentation runtime assembled around the legacy Processing game host.
 *
 * <p>This groups frame rendering, projected session views, button binding, and input dispatch into
 * one object so {@code Game} no longer needs to directly coordinate every UI-facing collaborator.
 * The extracted presentation coordinator still provides the authoritative hooks for projected
 * state and bot control availability.</p>
 */
public final class GameDesktopPresentationHost {
    private final GameDesktopPresentationCoordinator presentationCoordinator;
    private final GameDesktopShellDependencies shellDependencies;
    private final DebugPerformanceStats debugPerformanceStats;
    private final Supplier<GameSessionState> sessionStateSupplier;
    private final Supplier<Player> currentTurnPlayerSupplier;
    private final GamePrimaryTurnControls gamePrimaryTurnControls;
    private final GameSessionQueries gameSessionQueries;
    private final GameUiController gameUiController;
    private final GameFrameCoordinator gameFrameCoordinator;
    private final SessionViewFacade sessionViewFacade;

    public GameDesktopPresentationHost(
            GameDesktopPresentationCoordinator presentationCoordinator,
            GameDesktopShellDependencies shellDependencies,
            DebugPerformanceStats debugPerformanceStats,
            Supplier<GameSessionState> sessionStateSupplier,
            Supplier<Player> currentTurnPlayerSupplier,
            GamePrimaryTurnControls gamePrimaryTurnControls,
            GameSessionQueries gameSessionQueries,
            GameUiController gameUiController,
            GameFrameCoordinator gameFrameCoordinator
    ) {
        this.presentationCoordinator = presentationCoordinator;
        this.shellDependencies = shellDependencies;
        this.debugPerformanceStats = debugPerformanceStats;
        this.sessionStateSupplier = sessionStateSupplier;
        this.currentTurnPlayerSupplier = currentTurnPlayerSupplier;
        this.gamePrimaryTurnControls = gamePrimaryTurnControls;
        this.gameSessionQueries = gameSessionQueries;
        this.gameUiController = gameUiController;
        this.gameFrameCoordinator = gameFrameCoordinator;
        this.sessionViewFacade = presentationCoordinator.createSessionViewFacade(shellDependencies);
    }

    public void bindButtonActions() {
        gameUiController.bindButtonActions();
    }

    public GamePrimaryTurnControls primaryTurnControls() {
        return gamePrimaryTurnControls;
    }

    public GameSessionQueries gameSessionQueries() {
        return gameSessionQueries;
    }

    public void advancePresentationFrame() {
        gameFrameCoordinator.advancePresentationFrame(createFrameHooks());
    }

    public void render() {
        gameFrameCoordinator.renderFrame(createFrameHooks());
    }

    public boolean handleEvent(Event event) {
        updateLogTurnContext();
        return gameUiController.handleEvent(event);
    }

    public LayoutMetrics updateFrameLayoutMetrics() {
        return gameFrameCoordinator.updateFrameLayoutMetrics();
    }

    public LayoutMetrics getLayoutMetrics() {
        return gameFrameCoordinator.getLayoutMetrics();
    }

    public void updateSidebarControlPositions() {
        gameFrameCoordinator.updateSidebarControlPositions();
    }

    public void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        gameFrameCoordinator.updateSidebarControlPositions(layoutMetrics);
    }

    public float getSidebarHistoryHeight() {
        return gameFrameCoordinator.getSidebarHistoryHeight();
    }

    public float getSidebarHistoryPanelY() {
        return gameFrameCoordinator.getSidebarHistoryPanelY();
    }

    public float getSidebarReservedTop() {
        return gameFrameCoordinator.getSidebarReservedTop();
    }

    public GameSidebarPresenter.SidebarState createSidebarState() {
        return gameFrameCoordinator.createSidebarState(createFrameHooks());
    }

    public float getSidebarContentTop() {
        return gameFrameCoordinator.getSidebarContentTop(createFrameHooks());
    }

    public void updateDebtButtons(SessionState projectedSessionState) {
        gameFrameCoordinator.updateDebtButtons(shellDependencies.debtState(), projectedSessionState);
    }

    public void updateDebugButtons() {
        gameFrameCoordinator.updatePersistentButtons(sessionStateSupplier.get().gameOver());
    }

    public void refreshButtonInteractivityState() {
        gameFrameCoordinator.refreshButtonInteractivityState();
    }

    public void refreshLabels() {
        GameSessionState sessionState = sessionStateSupplier.get();
        gameFrameCoordinator.refreshLabels(sessionState.paused(), sessionState.botSpeedMode());
    }

    public void showRollDiceControl() {
        gamePrimaryTurnControls.showRollDiceControl();
    }

    public void showEndTurnControl() {
        gamePrimaryTurnControls.showEndTurnControl();
    }

    public void hidePrimaryTurnControls() {
        gamePrimaryTurnControls.hide();
    }

    public void updateLogTurnContext() {
        GameSessionState sessionState = sessionStateSupplier.get();
        gameFrameCoordinator.updateLogTurnContext(
                sessionState.gameOver(),
                sessionState.winner(),
                currentTurnPlayerSupplier.get()
        );
    }

    public void syncTransientPresentationState() {
        gameFrameCoordinator.syncTransientPresentationState(
                () -> presentationCoordinator.restoreBotTurnControlsIfNeeded(shellDependencies)
        );
    }

    public void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning) {
        gameFrameCoordinator.applyComputerActionCooldownIfAnimationJustFinished(
                animationWasRunning,
                createFrameHooks()
        );
    }

    public void enforcePrimaryTurnControlInvariant() {
        gameFrameCoordinator.enforcePrimaryTurnControlInvariant(shellDependencies.debtState() != null);
    }

    public GameView createGameView(Player currentPlayer) {
        long snapshotStart = System.nanoTime();
        GameView view = sessionViewFacade.createGameView(currentPlayer);
        debugPerformanceStats.recordGameViewBuild(System.nanoTime() - snapshotStart);
        return view;
    }

    public PlayerView createPlayerView(Player player) {
        return sessionViewFacade.createPlayerView(player);
    }

    public List<String> debugPerformanceLines(float fps) {
        return debugPerformanceStats.overlayLines(fps, DesktopImageCatalog.getColoredImageCopies());
    }

    public GameFrameCoordinator gameFrameCoordinator() {
        return gameFrameCoordinator;
    }

    private GameFrameCoordinator.FrameHooks createFrameHooks() {
        return presentationCoordinator.createFrameHooks(shellDependencies);
    }
}
