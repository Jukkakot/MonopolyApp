package fi.monopoly.application.session.debt;

import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResult;

public interface DebtOpeningGateway {
    PaymentResult tryResolve(PaymentRequest request);

    void openDebtState(PaymentRequest request, Runnable onResolved, PaymentResult result);
}
