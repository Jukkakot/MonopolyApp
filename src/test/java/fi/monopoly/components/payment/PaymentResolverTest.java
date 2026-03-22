package fi.monopoly.components.payment;

import fi.monopoly.components.Player;
import fi.monopoly.components.properties.RailRoadProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentResolverTest {

    private final PaymentResolver resolver = new PaymentResolver();

    @Test
    void tryPayPaysBankImmediatelyWhenCashIsEnough() {
        Player debtor = TestObjectFactory.player("Debtor", 500, 1);

        PaymentResult result = resolver.tryPay(new PaymentRequest(debtor, BankTarget.INSTANCE, 200, "Tax"));

        assertEquals(PaymentStatus.PAID, result.status());
        assertEquals(300, debtor.getMoneyAmount());
    }

    @Test
    void tryPayTransfersMoneyToPlayerWhenCashIsEnough() {
        Player debtor = TestObjectFactory.player("Debtor", 500, 1);
        Player creditor = TestObjectFactory.player("Creditor", 100, 2);

        PaymentResult result = resolver.tryPay(new PaymentRequest(debtor, new PlayerTarget(creditor), 200, "Rent"));

        assertEquals(PaymentStatus.PAID, result.status());
        assertEquals(300, debtor.getMoneyAmount());
        assertEquals(300, creditor.getMoneyAmount());
    }

    @Test
    void tryPayRequiresDebtResolutionWhenCashIsNotEnoughButAssetsCoverDebt() {
        Player debtor = TestObjectFactory.player("Debtor", 50, 1);
        var property = new RailRoadProperty(SpotType.RR1);
        TestObjectFactory.giveProperty(debtor, property);

        PaymentResult result = resolver.tryPay(new PaymentRequest(debtor, BankTarget.INSTANCE, 100, "Tax"));

        assertEquals(PaymentStatus.REQUIRES_DEBT_RESOLUTION, result.status());
        assertEquals(50, result.missingAmount());
        assertEquals(50, debtor.getMoneyAmount());
    }

    @Test
    void tryPayReturnsBankruptWhenCashAndAssetsDoNotCoverDebt() {
        Player debtor = TestObjectFactory.player("Debtor", 20, 1);

        PaymentResult result = resolver.tryPay(new PaymentRequest(debtor, BankTarget.INSTANCE, 100, "Tax"));

        assertEquals(PaymentStatus.BANKRUPT, result.status());
        assertEquals(80, result.missingAmount());
        assertEquals(20, debtor.getMoneyAmount());
    }
}
