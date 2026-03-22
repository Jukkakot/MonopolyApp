package fi.monopoly.components.payment;

import fi.monopoly.components.CallbackAction;

public interface PaymentHandler {
    void requestPayment(PaymentRequest request, CallbackAction onResolved);
}
