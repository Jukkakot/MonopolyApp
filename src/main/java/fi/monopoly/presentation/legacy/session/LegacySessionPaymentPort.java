package fi.monopoly.presentation.legacy.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.client.session.SessionPaymentPort;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.domain.session.TurnContinuationState;
import fi.monopoly.presentation.legacy.session.debt.DebtOpeningGateway;
import fi.monopoly.presentation.legacy.session.debt.RentAndDebtOpeningHandler;

/**
 * Legacy adapter that implements {@link SessionPaymentPort} using the Processing-era
 * {@link RentAndDebtOpeningHandler}.
 *
 * <p>Used exclusively on the legacy desktop path. The pure domain path
 * ({@code PureDomainSessionFactory}) handles rent and debt directly in
 * {@code DomainTurnActionGateway} and never touches this class.</p>
 */
public final class LegacySessionPaymentPort implements SessionPaymentPort {

    private final RentAndDebtOpeningHandler handler;

    public LegacySessionPaymentPort(SessionApplicationService service,
                                     DebtOpeningGateway debtOpeningGateway) {
        this.handler = new RentAndDebtOpeningHandler(
                service::setActiveDebtOverride,
                service::setTurnContinuationOverride,
                service::resumeContinuation,
                debtOpeningGateway
        );
    }

    @Override
    public void handlePaymentRequest(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        handler.handle(request, continuationState, onResolved);
    }
}
