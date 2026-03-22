package fi.monopoly.components;

import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResolver;
import fi.monopoly.components.payment.PaymentStatus;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameDebtTest {

    private final PaymentResolver resolver = new PaymentResolver();

    @Test
    void playerNeedsDebtResolutionWithoutManualMortgage() {
        Player player = TestObjectFactory.player("Debtor", 0, 1);
        TestObjectFactory.giveProperty(player, PropertyFactory.getProperty(SpotType.RR1));

        var result = resolver.tryPay(new PaymentRequest(player, BankTarget.INSTANCE, 100, "Test debt"));

        assertEquals(PaymentStatus.REQUIRES_DEBT_RESOLUTION, result.status());
        assertEquals(100, result.missingAmount());
        assertEquals(0, player.getMoneyAmount());
    }

    @Test
    void playerCanPayDebtAfterManualMortgage() {
        Player player = TestObjectFactory.player("Debtor", 0, 1);
        var property = PropertyFactory.getProperty(SpotType.RR1);
        TestObjectFactory.giveProperty(player, property);
        property.setMortgaged(true);
        player.addMoney(property.getMortgageValue());

        var result = resolver.tryPay(new PaymentRequest(player, BankTarget.INSTANCE, property.getMortgageValue(), "Test debt"));

        assertEquals(PaymentStatus.PAID, result.status());
        assertEquals(0, result.missingAmount());
        assertEquals(0, player.getMoneyAmount());
    }

    @Test
    void playerGoesBankruptWhenAssetsDoNotCoverDebt() {
        Player player = TestObjectFactory.player("Debtor", 0, 1);

        var result = resolver.tryPay(new PaymentRequest(player, BankTarget.INSTANCE, 100, "Test debt"));

        assertEquals(PaymentStatus.BANKRUPT, result.status());
        assertEquals(100, result.missingAmount());
        assertEquals(0, player.getMoneyAmount());
    }
}
