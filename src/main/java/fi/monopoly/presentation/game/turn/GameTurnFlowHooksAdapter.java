package fi.monopoly.presentation.game.turn;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.domain.session.TurnContinuationState;
import lombok.RequiredArgsConstructor;

import java.util.function.BooleanSupplier;

@RequiredArgsConstructor
public final class GameTurnFlowHooksAdapter implements GameTurnFlowCoordinator.Hooks {
    private final Runnable updateLogTurnContextAction;
    private final Runnable hidePrimaryTurnControlsAction;
    private final Runnable showRollDiceControlAction;
    private final Runnable showEndTurnControlAction;
    private final BooleanSupplier gameOverSupplier;
    private final BooleanSupplier debtActiveSupplier;
    private final BiCallbackPaymentHandler paymentHandler;

    @Override
    public void updateLogTurnContext() {
        updateLogTurnContextAction.run();
    }

    @Override
    public boolean gameOver() {
        return gameOverSupplier.getAsBoolean();
    }

    @Override
    public boolean debtActive() {
        return debtActiveSupplier.getAsBoolean();
    }

    @Override
    public void hidePrimaryTurnControls() {
        hidePrimaryTurnControlsAction.run();
    }

    @Override
    public void showRollDiceControl() {
        showRollDiceControlAction.run();
    }

    @Override
    public void showEndTurnControl() {
        showEndTurnControlAction.run();
    }

    @Override
    public void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        paymentHandler.handle(request, continuationState, onResolved);
    }

    @FunctionalInterface
    public interface BiCallbackPaymentHandler {
        void handle(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved);
    }
}
