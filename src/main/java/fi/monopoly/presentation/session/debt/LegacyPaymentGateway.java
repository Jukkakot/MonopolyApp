package fi.monopoly.presentation.session.debt;

import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResolver;
import fi.monopoly.components.payment.PaymentResult;
import fi.monopoly.components.payment.PaymentStatus;

public class LegacyPaymentGateway {
    private final PaymentResolver paymentResolver;
    private final DebtController debtController;

    public LegacyPaymentGateway(DebtController debtController) {
        this(new PaymentResolver(), debtController);
    }

    public LegacyPaymentGateway(PaymentResolver paymentResolver, DebtController debtController) {
        this.paymentResolver = paymentResolver;
        this.debtController = debtController;
    }

    public PaymentResult tryResolve(PaymentRequest request) {
        return paymentResolver.tryPay(request);
    }

    public void openDebtState(PaymentRequest request, Runnable onResolved, PaymentResult result) {
        debtController.openDebtState(request, onResolved, result.missingAmount(), result.status() == PaymentStatus.BANKRUPT);
    }
}
