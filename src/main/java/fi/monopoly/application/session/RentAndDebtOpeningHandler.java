package fi.monopoly.application.session;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResult;
import fi.monopoly.components.payment.PaymentStatus;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.domain.session.DebtAction;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.domain.session.PaymentObligation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class RentAndDebtOpeningHandler {
    private final Consumer<DebtStateModel> activeDebtUpdater;
    private final LegacyPaymentGateway paymentGateway;

    public RentAndDebtOpeningHandler(Consumer<DebtStateModel> activeDebtUpdater, LegacyPaymentGateway paymentGateway) {
        this.activeDebtUpdater = Objects.requireNonNull(activeDebtUpdater);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
    }

    public void handle(PaymentRequest request, CallbackAction onResolved) {
        PaymentResult result = paymentGateway.tryResolve(request);
        if (result.status() == PaymentStatus.PAID) {
            activeDebtUpdater.accept(null);
            onResolved.doAction();
            return;
        }

        boolean bankruptcyRisk = result.status() == PaymentStatus.BANKRUPT;
        activeDebtUpdater.accept(toDebtStateModel(request, result.missingAmount(), bankruptcyRisk));
        paymentGateway.openDebtState(request, onResolved, result);
    }

    private DebtStateModel toDebtStateModel(PaymentRequest request, int missingAmount, boolean bankruptcyRisk) {
        PaymentObligation obligation = new PaymentObligation(
                playerId(request.debtor()),
                request.target() instanceof PlayerTarget ? DebtCreditorType.PLAYER : DebtCreditorType.BANK,
                request.target() instanceof PlayerTarget playerTarget ? playerId(playerTarget.player()) : null,
                request.amount(),
                request.reason()
        );
        return new DebtStateModel(
                "debt:" + obligation.debtorPlayerId() + ":" + obligation.amount() + ":" + obligation.reason().hashCode(),
                obligation.debtorPlayerId(),
                obligation.creditorType(),
                obligation.creditorPlayerId(),
                missingAmount,
                obligation.reason(),
                bankruptcyRisk,
                allowedActions(bankruptcyRisk)
        );
    }

    private List<DebtAction> allowedActions(boolean bankruptcyRisk) {
        List<DebtAction> actions = new ArrayList<>(List.of(
                DebtAction.PAY_DEBT_NOW,
                DebtAction.MORTGAGE_PROPERTY,
                DebtAction.SELL_BUILDING
        ));
        if (bankruptcyRisk) {
            actions.add(DebtAction.DECLARE_BANKRUPTCY);
        }
        return actions;
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }
}
