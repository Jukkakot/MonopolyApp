package fi.monopoly.presentation.legacy.session.debt;

import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResult;

/**
 * Legacy bridge gateway for opening debt resolution via the Processing desktop runtime.
 *
 * <p>Implementations live in the presentation layer and depend on legacy component types.
 * Not used by the pure domain session path ({@code PureDomainSessionFactory}).</p>
 */
public interface DebtOpeningGateway {
    PaymentResult tryResolve(PaymentRequest request);

    void openDebtState(PaymentRequest request, Runnable onResolved, PaymentResult result);
}
