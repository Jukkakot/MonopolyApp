package fi.monopoly.components.payment;

import fi.monopoly.components.Player;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaymentResolver {

    public PaymentResult tryPay(PaymentRequest request) {
        Player debtor = request.debtor();
        int amount = request.amount();
        log.debug("Resolving payment: debtor={}, target={}, amount={}, reason={}, cash={}, liquidationValue={}",
                debtor.getName(), request.target().getDisplayName(), amount, request.reason(),
                debtor.getMoneyAmount(), debtor.getTotalLiquidationValue());

        if (debtor.addMoney(-amount)) {
            transferToTarget(request.target(), amount);
            log.info("Payment paid immediately: debtor={}, target={}, amount={}",
                    debtor.getName(), request.target().getDisplayName(), amount);
            return new PaymentResult(PaymentStatus.PAID, 0);
        }

        int missingAmount = Math.max(0, amount - debtor.getMoneyAmount());
        if (canRaiseFunds(debtor, amount)) {
            log.info("Payment requires debt resolution: debtor={}, target={}, amount={}, missing={}",
                    debtor.getName(), request.target().getDisplayName(), amount, missingAmount);
            return new PaymentResult(PaymentStatus.REQUIRES_DEBT_RESOLUTION, missingAmount);
        }

        log.warn("Payment would cause bankruptcy: debtor={}, target={}, amount={}, missing={}, cash={}, liquidationValue={}",
                debtor.getName(), request.target().getDisplayName(), amount, missingAmount,
                debtor.getMoneyAmount(), debtor.getTotalLiquidationValue());
        return new PaymentResult(PaymentStatus.BANKRUPT, missingAmount);
    }

    private void transferToTarget(PaymentTarget target, int amount) {
        if (target instanceof PlayerTarget playerTarget) {
            playerTarget.player().addMoney(amount);
            log.trace("Transferred M{} to player {}", amount, playerTarget.player().getName());
        }
    }

    private boolean canRaiseFunds(Player player, int requiredAmount) {
        return player.getMoneyAmount() + player.getTotalLiquidationValue() >= requiredAmount;
    }
}
