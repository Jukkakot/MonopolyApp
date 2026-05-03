package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.presentation.session.debt.DebtController;

/**
 * Reattaches authoritative session state to the current desktop presentation/runtime shell.
 *
 * <p>Loading a saved game currently requires more than rebuilding runtime objects: the
 * application-layer overrides, debt controls, and primary turn controls also need to be restored
 * into a coherent UI state. This coordinator centralizes that reattachment logic so it can be
 * evolved or replaced independently of {@code Game}.</p>
 */
public final class RestoredSessionReattachmentCoordinator {

    public RestoredGameState restoreAuthoritativeState(
            SessionState restoredSessionState,
            SessionPresentationStatePort sessionPresentationState
    ) {
        if (restoredSessionState == null) {
            return new RestoredGameState(false, false);
        }
        sessionPresentationState.restoreFrom(restoredSessionState);
        boolean paused = restoredSessionState.status() == SessionStatus.PAUSED;
        boolean gameOver = restoredSessionState.status() == SessionStatus.GAME_OVER
                || restoredSessionState.winnerPlayerId() != null;
        return new RestoredGameState(paused, gameOver);
    }

    public void restorePresentation(
            SessionState restoredSessionState,
            SessionPresentationStatePort sessionPresentationState,
            DebtController debtController,
            Hooks hooks
    ) {
        hooks.refreshLabels();
        if (restoredSessionState == null) {
            hooks.showRollDiceControl();
            return;
        }
        restoreDebtPresentationState(restoredSessionState, debtController, hooks);
        hooks.updateDebtButtons();
        hooks.syncTransientPresentationState();
        restorePrimaryTurnControls(restoredSessionState, hooks);
    }

    private void restoreDebtPresentationState(
            SessionState restoredSessionState,
            DebtController debtController,
            Hooks hooks
    ) {
        if (debtController == null || restoredSessionState.activeDebt() == null) {
            return;
        }
        debtController.restoreDebtStateFromModel(
                restoredSessionState.activeDebt(),
                () -> hooks.resumeContinuation(restoredSessionState.turnContinuationState())
        );
    }

    private void restorePrimaryTurnControls(SessionState state, Hooks hooks) {
        if (hooks.gameOver() || state == null) {
            hooks.hidePrimaryTurnControls();
            return;
        }
        if (state.activeDebt() != null
                || state.pendingDecision() != null
                || state.auctionState() != null
                || state.tradeState() != null) {
            hooks.hidePrimaryTurnControls();
            return;
        }
        if (state.turn() != null && state.turn().canEndTurn()) {
            hooks.showEndTurnControl();
            return;
        }
        if (state.turn() != null && state.turn().canRoll()) {
            hooks.showRollDiceControl();
            return;
        }
        hooks.hidePrimaryTurnControls();
    }

    public interface Hooks {
        boolean gameOver();

        void refreshLabels();

        void showRollDiceControl();

        void showEndTurnControl();

        void hidePrimaryTurnControls();

        void updateDebtButtons();

        void syncTransientPresentationState();

        void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState);
    }

    public record RestoredGameState(boolean paused, boolean gameOver) {
    }
}
