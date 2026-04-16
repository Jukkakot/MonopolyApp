package fi.monopoly.components.payment;

public record DebtState(
        PaymentRequest paymentRequest,
        Runnable onResolved,
        boolean bankruptcyRisk
) {
}
