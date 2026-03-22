package fi.monopoly.components.payment;

public sealed interface PaymentTarget permits BankTarget, PlayerTarget {
    String getDisplayName();
}
