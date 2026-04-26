package fi.monopoly.application.session.debt;

import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.payment.PaymentResult;
import fi.monopoly.components.payment.PaymentStatus;
import fi.monopoly.components.payment.PlayerTarget;
import fi.monopoly.domain.session.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class RentAndDebtOpeningHandler {
    private final Consumer<DebtStateModel> activeDebtUpdater;
    private final Consumer<TurnContinuationState> turnContinuationUpdater;
    private final Consumer<TurnContinuationState> turnContinuationResolver;
    private final DebtOpeningGateway paymentGateway;

    public void handle(PaymentRequest request, TurnContinuationState continuationState, CallbackAction onResolved) {
        PaymentResult result = paymentGateway.tryResolve(request);
        if (result.status() == PaymentStatus.PAID) {
            activeDebtUpdater.accept(null);
            turnContinuationUpdater.accept(null);
            if (continuationState != null) {
                turnContinuationResolver.accept(continuationState);
            } else {
                onResolved.doAction();
            }
            return;
        }

        boolean bankruptcyRisk = result.status() == PaymentStatus.BANKRUPT;
        activeDebtUpdater.accept(toDebtStateModel(request, result.missingAmount(), bankruptcyRisk));
        turnContinuationUpdater.accept(continuationState);
        paymentGateway.openDebtState(
                request,
                continuationState != null
                        ? () -> turnContinuationResolver.accept(continuationState)
                        : onResolved::doAction,
                result
        );
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
                obligation.amount(),
                obligation.reason(),
                bankruptcyRisk,
                request.debtor().getMoneyAmount(),
                request.debtor().getTotalLiquidationValue(),
                allowedActions(bankruptcyRisk)
        );
    }

    private List<DebtAction> allowedActions(boolean bankruptcyRisk) {
        List<DebtAction> actions = new ArrayList<>(List.of(
                DebtAction.PAY_DEBT_NOW,
                DebtAction.MORTGAGE_PROPERTY,
                DebtAction.SELL_BUILDING,
                DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET
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
