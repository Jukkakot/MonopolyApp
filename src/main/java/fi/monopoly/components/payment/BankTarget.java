package fi.monopoly.components.payment;

import static fi.monopoly.text.UiTexts.text;

public final class BankTarget implements PaymentTarget {
    public static final BankTarget INSTANCE = new BankTarget();

    private BankTarget() {
    }

    @Override
    public String getDisplayName() {
        return text("payment.target.bank");
    }
}
