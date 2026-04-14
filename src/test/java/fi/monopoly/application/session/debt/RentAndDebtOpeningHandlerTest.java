package fi.monopoly.application.session.debt;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.payment.BankTarget;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResolver;
import fi.monopoly.components.payment.PaymentResult;
import fi.monopoly.components.payment.PaymentStatus;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.domain.session.DebtAction;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.TurnContinuationAction;
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.domain.session.TurnContinuationType;
import fi.monopoly.presentation.session.debt.LegacyPaymentGateway;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RentAndDebtOpeningHandlerTest {

    @BeforeEach
    void resetPropertyFactory() {
        PropertyFactory.resetState();
    }

    @Test
    void immediatePaymentDoesNotOpenDebt() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 500, 1, ComputerPlayerProfile.HUMAN);
        AtomicReference<DebtStateModel> debtRef = new AtomicReference<>();
        AtomicReference<TurnContinuationState> continuationRef = new AtomicReference<>();
        AtomicBoolean callbackTriggered = new AtomicBoolean(false);
        FakePaymentGateway paymentGateway = new FakePaymentGateway(new PaymentResolver());
        RentAndDebtOpeningHandler handler = new RentAndDebtOpeningHandler(debtRef::set, continuationRef::set, paymentGateway);

        handler.handle(new PaymentRequest(debtor, BankTarget.INSTANCE, 100, "Rent"), continuationFor(debtor), () -> callbackTriggered.set(true));

        assertTrue(callbackTriggered.get());
        assertNull(debtRef.get());
        assertNull(continuationRef.get());
        assertFalse(paymentGateway.openCalled);
        assertEquals(400, debtor.getMoneyAmount());
    }

    @Test
    void insufficientCashOpensDebtWithoutBankruptcyRisk() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 0, 1, ComputerPlayerProfile.HUMAN);
        debtor.addOwnedProperty(PropertyFactory.getProperty(SpotType.RR1));
        AtomicReference<DebtStateModel> debtRef = new AtomicReference<>();
        AtomicReference<TurnContinuationState> continuationRef = new AtomicReference<>();
        FakePaymentGateway paymentGateway = new FakePaymentGateway(new PaymentResolver());
        RentAndDebtOpeningHandler handler = new RentAndDebtOpeningHandler(debtRef::set, continuationRef::set, paymentGateway);

        handler.handle(new PaymentRequest(debtor, BankTarget.INSTANCE, 100, "Rent"), continuationFor(debtor), () -> {
        });

        assertNotNull(debtRef.get());
        assertEquals(continuationFor(debtor), continuationRef.get());
        assertEquals(100, debtRef.get().amountRemaining());
        assertFalse(debtRef.get().bankruptcyRisk());
        assertEquals(List.of(DebtAction.PAY_DEBT_NOW, DebtAction.MORTGAGE_PROPERTY, DebtAction.SELL_BUILDING, DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET), debtRef.get().allowedActions());
        assertEquals(0, debtRef.get().currentCash());
        assertTrue(paymentGateway.openCalled);
        assertEquals(PaymentStatus.REQUIRES_DEBT_RESOLUTION, paymentGateway.lastStatus);
    }

    @Test
    void impossiblePaymentOpensDebtWithBankruptcyRisk() {
        Player debtor = new Player("Debtor", Color.MEDIUMPURPLE, 0, 1, ComputerPlayerProfile.HUMAN);
        AtomicReference<DebtStateModel> debtRef = new AtomicReference<>();
        AtomicReference<TurnContinuationState> continuationRef = new AtomicReference<>();
        FakePaymentGateway paymentGateway = new FakePaymentGateway(new PaymentResolver());
        RentAndDebtOpeningHandler handler = new RentAndDebtOpeningHandler(debtRef::set, continuationRef::set, paymentGateway);

        handler.handle(new PaymentRequest(debtor, BankTarget.INSTANCE, 100, "Rent"), continuationFor(debtor), () -> {
        });

        assertNotNull(debtRef.get());
        assertTrue(debtRef.get().bankruptcyRisk());
        assertEquals(continuationFor(debtor), continuationRef.get());
        assertEquals(List.of(DebtAction.PAY_DEBT_NOW, DebtAction.MORTGAGE_PROPERTY, DebtAction.SELL_BUILDING, DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET, DebtAction.DECLARE_BANKRUPTCY), debtRef.get().allowedActions());
        assertTrue(paymentGateway.openCalled);
        assertEquals(PaymentStatus.BANKRUPT, paymentGateway.lastStatus);
    }

    private TurnContinuationState continuationFor(Player debtor) {
        return new TurnContinuationState(
                "continuation:rent:" + debtor.getId(),
                "player-" + debtor.getId(),
                TurnContinuationType.RESUME_AFTER_DEBT,
                TurnContinuationAction.APPLY_TURN_FOLLOW_UP,
                null,
                "resume-after-debt"
        );
    }

    private static final class FakePaymentGateway extends LegacyPaymentGateway {
        private boolean openCalled;
        private PaymentStatus lastStatus;

        private FakePaymentGateway(PaymentResolver paymentResolver) {
            super(paymentResolver, null);
        }

        @Override
        public void openDebtState(PaymentRequest request, fi.monopoly.components.CallbackAction onResolved, PaymentResult result) {
            openCalled = true;
            lastStatus = result.status();
        }
    }
}
