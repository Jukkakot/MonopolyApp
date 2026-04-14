package fi.monopoly.components.payment;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.domain.session.TurnContinuationState;

public interface PaymentHandler {
    void requestPayment(PaymentRequest request, CallbackAction onResolved);

    default void requestPayment(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        requestPayment(request, onResolved);
    }
}
