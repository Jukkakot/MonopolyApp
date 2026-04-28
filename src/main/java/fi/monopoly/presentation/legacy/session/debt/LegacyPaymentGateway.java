package fi.monopoly.presentation.legacy.session.debt;

import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResolver;
import fi.monopoly.components.payment.PaymentResult;
import fi.monopoly.components.payment.PaymentStatus;
import fi.monopoly.presentation.session.debt.DebtController;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LegacyPaymentGateway implements DebtOpeningGateway {
    private final PaymentResolver paymentResolver;
    private final DebtController debtController;

    public LegacyPaymentGateway(DebtController debtController) {
        this(new PaymentResolver(), debtController);
    }

    public PaymentResult tryResolve(PaymentRequest request) {
        return paymentResolver.tryPay(request);
    }

    public void openDebtState(PaymentRequest request, Runnable onResolved, PaymentResult result) {
        debtController.openDebtState(request, onResolved, result.missingAmount(), result.status() == PaymentStatus.BANKRUPT);
    }
}
