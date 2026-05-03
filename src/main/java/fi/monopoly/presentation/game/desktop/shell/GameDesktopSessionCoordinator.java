package fi.monopoly.presentation.game.desktop.shell;

import fi.monopoly.components.Player;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.game.desktop.session.GameSessionBridgeFactory;
import fi.monopoly.presentation.game.desktop.session.RestoredSessionReattachmentCoordinator;
import fi.monopoly.presentation.game.session.GameSessionState;
import fi.monopoly.presentation.game.session.GameSessionStateCoordinator;
import lombok.RequiredArgsConstructor;

/**
 * Session/restore coordinator for the desktop-local shell.
 *
 * <p>This owns session-facing bridge and reattachment concerns: current turn/session snapshots,
 * restored state application, and persistence notice lifecycle. It deliberately stays separate
 * from frame/UI coordination so the desktop shell no longer collapses both concerns into one
 * coordinator.</p>
 */
@RequiredArgsConstructor
public final class GameDesktopSessionCoordinator {
    private final fi.monopoly.client.desktop.MonopolyRuntime runtime;
    private final GameSessionStateCoordinator sessionStateCoordinator;

    public GameSessionBridgeFactory.Hooks createSessionBridgeHooks(GameDesktopShellDependencies dependencies) {
        return new GameSessionBridgeFactory.Hooks() {
            @Override
            public boolean paused() {
                return dependencies.sessionState().paused();
            }

            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public String winnerPlayerId() {
                return dependencies.sessionState().winnerPlayerId();
            }

            @Override
            public boolean projectedRollDiceActionAvailable() {
                return dependencies.projectedRollDiceActionAvailable();
            }

            @Override
            public boolean projectedEndTurnActionAvailable() {
                return dependencies.projectedEndTurnActionAvailable();
            }

            @Override
            public void endTurn() {
                dependencies.endRound(true);
            }

            @Override
            public boolean computerTurn() {
                fi.monopoly.domain.session.SessionState state = dependencies.sessionCommandPort().currentState();
                if (state == null || state.turn() == null || state.turn().activePlayerId() == null) return false;
                String activeId = state.turn().activePlayerId();
                return state.seats().stream().anyMatch(s -> activeId.equals(s.playerId()) && s.seatKind() == fi.monopoly.domain.session.SeatKind.BOT);
            }

            @Override
            public boolean canOpenTrade() {
                return !dependencies.sessionState().gameOver()
                        && !dependencies.popupService().isAnyVisible()
                        && dependencies.debtState() == null;
            }
        };
    }

    public RestoredSessionReattachmentCoordinator.Hooks createRestoredSessionReattachmentHooks(
            GameDesktopShellDependencies dependencies
    ) {
        return new RestoredSessionReattachmentCoordinator.Hooks() {
            @Override
            public Player playerById(String playerId) {
                return dependencies.playerById(playerId);
            }

            @Override
            public boolean gameOver() {
                return dependencies.sessionState().gameOver();
            }

            @Override
            public void refreshLabels() {
                dependencies.refreshLabels();
            }

            @Override
            public void showRollDiceControl() {
                dependencies.showRollDiceControl();
            }

            @Override
            public void showEndTurnControl() {
                dependencies.showEndTurnControl();
            }

            @Override
            public void hidePrimaryTurnControls() {
                dependencies.hidePrimaryTurnControls();
            }

            @Override
            public void updateDebtButtons() {
                dependencies.updateDebtButtons();
            }

            @Override
            public void syncTransientPresentationState() {
                dependencies.syncTransientPresentationState();
            }

            @Override
            public void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState) {
                dependencies.resumeContinuation(continuationState);
            }
        };
    }

    public void applyRestoredSessionState(GameDesktopShellDependencies dependencies, SessionState restoredSessionState) {
        sessionStateCoordinator.restoreSessionState(
                dependencies.sessionState(),
                restoredSessionState,
                dependencies.sessionPresentationState()
        );
    }

    public void initializeSessionPresentation(GameDesktopShellDependencies dependencies, SessionState restoredSessionState) {
        sessionStateCoordinator.initializePresentation(
                restoredSessionState,
                dependencies.sessionPresentationState(),
                dependencies.debtController(),
                createRestoredSessionReattachmentHooks(dependencies)
        );
    }

    public void showPersistenceNotice(GameSessionState sessionState, String notice) {
        sessionStateCoordinator.showPersistenceNotice(sessionState, notice, runtime.millis());
    }
}
