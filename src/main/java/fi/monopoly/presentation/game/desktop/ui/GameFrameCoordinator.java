package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.animation.Animation;
import fi.monopoly.components.animation.Animations;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import fi.monopoly.utils.DebugPerformanceStats;
import fi.monopoly.utils.LayoutMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public final class GameFrameCoordinator {
    private final MonopolyRuntime runtime;
    private final GameControlLayout gameControlLayout;
    private final GameSidebarPresenter gameSidebarPresenter;
    private final GamePresentationSupport gamePresentationSupport;
    private final GamePrimaryTurnControls gamePrimaryTurnControls;
    private final GameSidebarStateFactory gameSidebarStateFactory;
    private final GameSessionStateCoordinator gameSessionStateCoordinator;
    private final BotTurnScheduler botTurnScheduler;
    private final DebugPerformanceStats debugPerformanceStats;
    private final List<MonopolyButton> buttons;
    private LayoutMetrics frameLayoutMetrics;
    private long lastAnimationUpdateNanos = -1L;

    public void advancePresentationFrame(FrameHooks hooks) {
        updateLogTurnContext(hooks.sessionState().gameOver(), hooks.sessionState().winner(), hooks.turnPlayer());
        boolean animationWasRunning = hooks.animations().isRunning();
        float animationDeltaSeconds = resolveAnimationDeltaSeconds(System.nanoTime());
        if (DesktopClientSettings.skipAnimations()) {
            hooks.animations().finishAllAnimations();
        }
        if (!hooks.popupVisible()) {
            hooks.animations().updateAnimations(animationDeltaSeconds);
        }
        applyComputerActionCooldownIfAnimationJustFinishedInternal(animationWasRunning, hooks);
        LayoutMetrics layoutMetrics = updateFrameLayoutMetrics();
        updateSidebarControlPositions(layoutMetrics);
        refreshButtonInteractivityState();
        updatePersistentButtons(hooks.sessionState().gameOver());
        enforcePrimaryTurnControlInvariant(hooks.debtState() != null);
        syncTransientPresentationState(hooks::restoreBotTurnControlsIfNeeded);
    }

    public void renderFrame(FrameHooks hooks) {
        LayoutMetrics layoutMetrics = getLayoutMetrics();
        boolean hasSidebarSpace = layoutMetrics.hasSidebarSpace();
        GameSidebarPresenter.SidebarState sidebarState = createSidebarState(hooks);
        hooks.board().draw(null);
        if (hasSidebarSpace) {
            gameSidebarPresenter.drawSidebarPanel(layoutMetrics, sidebarState, debugPerformanceStats::recordHistoryLayout);
        }
        if (!hooks.debtSidebarMode()) {
            hooks.dices().draw(null);
        }
        if (hasSidebarSpace && hooks.debtSidebarMode() && hooks.debtDebtor() != null) {
            hooks.focusPlayer(hooks.debtDebtor());
        }
        if (hasSidebarSpace) {
            hooks.players().draw(
                    gameSidebarPresenter.contentTop(layoutMetrics, sidebarState),
                    !hooks.debtSidebarMode(),
                    !hooks.debtSidebarMode()
            );
            gameSidebarPresenter.drawDebtState(layoutMetrics, sidebarState.debtState());
            return;
        }
        hooks.players().drawTokens();
    }

    public LayoutMetrics updateFrameLayoutMetrics() {
        frameLayoutMetrics = gameControlLayout.updateFrameLayoutMetrics();
        return frameLayoutMetrics;
    }

    public LayoutMetrics getLayoutMetrics() {
        return frameLayoutMetrics != null
                ? frameLayoutMetrics
                : LayoutMetrics.fromWindow(runtime.windowWidth(), runtime.windowHeight());
    }

    public void updateSidebarControlPositions() {
        updateSidebarControlPositions(updateFrameLayoutMetrics());
    }

    public void updateSidebarControlPositions(LayoutMetrics layoutMetrics) {
        gameControlLayout.updateSidebarControlPositions(layoutMetrics);
    }

    public float getSidebarHistoryHeight() {
        return gameControlLayout.historyHeight(getLayoutMetrics());
    }

    public float getSidebarHistoryPanelY() {
        return gameControlLayout.historyPanelY(getLayoutMetrics());
    }

    public float getSidebarReservedTop() {
        return gameControlLayout.reservedTop(getLayoutMetrics());
    }

    public float getSidebarContentTop(FrameHooks hooks) {
        return gameSidebarPresenter.contentTop(getLayoutMetrics(), createSidebarState(hooks));
    }

    public GameSidebarPresenter.SidebarState createSidebarState(FrameHooks hooks) {
        gameSessionStateCoordinator.clearExpiredPersistenceNoticeIfNeeded(hooks.sessionState(), runtime.millis());
        return gameSidebarStateFactory.createSidebarState(
                hooks.turnPlayer(),
                hooks.players().getPlayers(),
                hooks.recentPopupMessages(),
                hooks.debtState(),
                hooks.sessionState().persistenceNotice(),
                hooks.sessionState().gameOver(),
                hooks.popupVisible(),
                hooks.animations().isRunning(),
                hooks.endRoundVisible(),
                hooks.rollDiceVisible(),
                hooks.authoritativeSessionState(),
                getSidebarHistoryPanelY(),
                getSidebarHistoryHeight(),
                getSidebarReservedTop()
        );
    }

    public void updateDebtButtons(DebtState debtState, SessionState sessionState) {
        gamePresentationSupport.updateDebtButtons(debtState, sessionState);
    }

    public void updatePersistentButtons(boolean gameOver) {
        gamePresentationSupport.updatePersistentButtons(gameOver);
    }

    public void refreshButtonInteractivityState() {
        for (MonopolyButton button : buttons) {
            button.refreshInteractivityStyle();
        }
    }

    public void refreshLabels(boolean paused, BotTurnScheduler.SpeedMode botSpeedMode) {
        gamePresentationSupport.refreshLabels(paused, botSpeedMode);
    }

    public void updateLogTurnContext(boolean gameOver, Player winner, Player turnPlayer) {
        gamePresentationSupport.updateLogTurnContext(gameOver, winner, turnPlayer);
    }

    public void syncTransientPresentationState(Runnable restoreBotTurnControlsIfNeeded) {
        gamePresentationSupport.syncTransientPresentationState();
        restoreBotTurnControlsIfNeeded.run();
    }

    public void applyComputerActionCooldownIfAnimationJustFinished(boolean animationWasRunning, FrameHooks hooks) {
        applyComputerActionCooldownIfAnimationJustFinishedInternal(animationWasRunning, hooks);
    }

    private static void logPrimaryTurnControlViolation() {
        log.warn("Primary turn controls were both visible. Hiding roll dice button to keep end-turn state authoritative.");
    }

    public void enforcePrimaryTurnControlInvariant(boolean debtActive) {
        gamePrimaryTurnControls.enforceInvariant(
                debtActive,
                GameFrameCoordinator::logPrimaryTurnControlViolation
        );
    }

    private float resolveAnimationDeltaSeconds(long nowNanos) {
        if (lastAnimationUpdateNanos < 0L) {
            lastAnimationUpdateNanos = nowNanos;
            return Animation.REFERENCE_FRAME_SECONDS;
        }
        float deltaSeconds = (nowNanos - lastAnimationUpdateNanos) / 1_000_000_000f;
        lastAnimationUpdateNanos = nowNanos;
        return deltaSeconds;
    }

    private void applyComputerActionCooldownIfAnimationJustFinishedInternal(boolean animationWasRunning, FrameHooks hooks) {
        Player turnPlayer = hooks.turnPlayer();
        botTurnScheduler.applyAnimationFinishCooldownIfNeeded(
                animationWasRunning,
                hooks.animations().isRunning(),
                turnPlayer != null && turnPlayer.isComputerControlled(),
                runtime.millis(),
                hooks.sessionState().botSpeedMode(),
                hooks.players().getPlayers().stream().allMatch(Player::isComputerControlled)
        );
    }

    public interface FrameHooks {
        GameSessionState sessionState();

        SessionState authoritativeSessionState();

        Board board();

        Players players();

        Dices dices();

        Animations animations();

        Player turnPlayer();

        List<String> recentPopupMessages();

        DebtState debtState();

        Player debtDebtor();

        boolean popupVisible();

        boolean debtSidebarMode();

        boolean endRoundVisible();

        boolean rollDiceVisible();

        void focusPlayer(Player player);

        void restoreBotTurnControlsIfNeeded();
    }
}
