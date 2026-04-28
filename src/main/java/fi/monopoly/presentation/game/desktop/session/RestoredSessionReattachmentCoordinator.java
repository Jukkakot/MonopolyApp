package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentTarget;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.presentation.session.debt.DebtController;

import java.util.function.Function;

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
            SessionPresentationStatePort sessionPresentationState,
            Function<String, Player> playerById
    ) {
        if (restoredSessionState == null) {
            return new RestoredGameState(false, false, null);
        }
        sessionPresentationState.restoreFrom(restoredSessionState);
        boolean paused = restoredSessionState.status() == SessionStatus.PAUSED;
        boolean gameOver = restoredSessionState.status() == SessionStatus.GAME_OVER
                || restoredSessionState.winnerPlayerId() != null;
        Player winner = restoredSessionState.winnerPlayerId() == null
                ? null
                : playerById.apply(restoredSessionState.winnerPlayerId());
        return new RestoredGameState(paused, gameOver, winner);
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
        PaymentRequest request = toRestoredPaymentRequest(restoredSessionState, hooks::playerById);
        if (request == null) {
            return;
        }
        debtController.restoreDebtState(
                request,
                () -> hooks.resumeContinuation(restoredSessionState.turnContinuationState()),
                restoredSessionState.activeDebt().bankruptcyRisk()
        );
    }

    private PaymentRequest toRestoredPaymentRequest(
            SessionState restoredSessionState,
            Function<String, Player> playerById
    ) {
        var activeDebt = restoredSessionState.activeDebt();
        if (activeDebt == null) {
            return null;
        }
        Player debtor = playerById.apply(activeDebt.debtorPlayerId());
        if (debtor == null) {
            return null;
        }
        PaymentTarget target = activeDebt.creditorType() == DebtCreditorType.PLAYER
                ? creditorTarget(activeDebt.creditorPlayerId(), playerById)
                : BankTarget.INSTANCE;
        if (target == null) {
            return null;
        }
        return new PaymentRequest(debtor, target, activeDebt.amountRemaining(), activeDebt.reason());
    }

    private PaymentTarget creditorTarget(
            String creditorPlayerId,
            Function<String, Player> playerById
    ) {
        Player creditor = playerById.apply(creditorPlayerId);
        return creditor == null ? null : new PlayerTarget(creditor);
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
        Player playerById(String playerId);

        boolean gameOver();

        void refreshLabels();

        void showRollDiceControl();

        void showEndTurnControl();

        void hidePrimaryTurnControls();

        void updateDebtButtons();

        void syncTransientPresentationState();

        void resumeContinuation(fi.monopoly.domain.session.TurnContinuationState continuationState);
    }

    public record RestoredGameState(boolean paused, boolean gameOver, Player winner) {
    }
}
