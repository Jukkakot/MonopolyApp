package fi.monopoly.components.payment;

public record PaymentResult(
        PaymentStatus status,
        int missingAmount
) {
}
