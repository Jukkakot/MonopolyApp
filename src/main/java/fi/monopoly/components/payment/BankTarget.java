package fi.monopoly.components.payment;

public final class BankTarget implements PaymentTarget {
    public static final BankTarget INSTANCE = new BankTarget();

    private BankTarget() {
    }

    @Override
    public String getDisplayName() {
        return "Bank";
    }
}
