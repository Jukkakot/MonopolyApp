package fi.monopoly.components.payment;

import fi.monopoly.components.Player;

public record PaymentRequest(
        Player debtor,
        PaymentTarget target,
        int amount,
        String reason
) {
}
