package fi.monopoly.client.session;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.domain.session.TurnContinuationState;

/**
 * Narrow port for opening the rent-and-debt payment flow from the presentation layer.
 *
 * <p>This keeps presentation-layer coordinators from depending on the full
 * application service when they only need to trigger payment handling.
 * Implemented by the legacy adapter {@code LegacySessionPaymentPort} (presentation layer).</p>
 */
@FunctionalInterface
public interface SessionPaymentPort {
    void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved);
}
