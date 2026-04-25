package fi.monopoly.application.session;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.domain.session.TurnContinuationState;

/**
 * Narrow port for opening the rent-and-debt payment flow from the presentation layer.
 *
 * <p>This keeps presentation-layer coordinators from depending on the full
 * {@link SessionApplicationService} when they only need to trigger payment handling.</p>
 */
@FunctionalInterface
public interface SessionPaymentPort {
    void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved);
}
