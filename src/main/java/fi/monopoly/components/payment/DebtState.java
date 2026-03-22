package fi.monopoly.components.payment;

import fi.monopoly.components.CallbackAction;

public record DebtState(
        PaymentRequest paymentRequest,
        CallbackAction onResolved,
        boolean bankruptcyRisk
) {
}
