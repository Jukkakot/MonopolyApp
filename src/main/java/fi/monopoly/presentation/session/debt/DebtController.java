package fi.monopoly.presentation.session.debt;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.payment.*;
import fi.monopoly.domain.session.DebtCreditorType;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.turn.PropertyAuctionResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
/**
 * Handles desktop debt-resolution flow around the legacy player runtime.
 *
 * <p>This controller owns popup messaging, asset liquidation, and bankruptcy progression while the
 * payment state itself is gradually moved behind application/session ports.</p>
 */
@RequiredArgsConstructor
public final class DebtController {
    private final MonopolyRuntime runtime;
    private final Players players;
    private final PaymentResolver paymentResolver = new PaymentResolver();
    private final Runnable hidePrimaryTurnControls;
    private final Runnable showRollDiceControl;
    private final Runnable onStateChanged;
    private final Consumer<String> declareWinner;

    private DebtState debtState;

    public DebtState debtState() {
        return debtState;
    }

    public void setDebtStateForTest(DebtState debtState) {
        this.debtState = debtState;
        onStateChanged.run();
    }

    public void clearDebtState() {
        debtState = null;
        onStateChanged.run();
    }

    public void handlePaymentRequest(PaymentRequest request, CallbackAction onResolved) {
        log.debug("Handling payment request in Game: debtor={}, target={}, amount={}, reason={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), request.reason());
        PaymentResult result = paymentResolver.tryPay(request);
        if (result.status() == PaymentStatus.PAID) {
            log.trace("Payment completed immediately in Game");
            onResolved.doAction();
            return;
        }

        debtState = new DebtState(request, onResolved::doAction, result.status() == PaymentStatus.BANKRUPT);
        hidePrimaryTurnControls.run();
        onStateChanged.run();
        log.info("Entering debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    public void openDebtState(PaymentRequest request, Runnable onResolved, int missingAmount, boolean bankruptcyRisk) {
        debtState = new DebtState(request, onResolved, bankruptcyRisk);
        hidePrimaryTurnControls.run();
        onStateChanged.run();
        log.info("Entering debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), bankruptcyRisk);
        runtime.popupService().show(buildDebtMessage(request, missingAmount, bankruptcyRisk));
    }

    public boolean restoreDebtStateFromModel(DebtStateModel activeDebt, Runnable onResolved) {
        if (activeDebt == null) {
            return false;
        }
        Player debtor = playerById(activeDebt.debtorPlayerId());
        if (debtor == null) {
            return false;
        }
        PaymentTarget target = activeDebt.creditorType() == DebtCreditorType.PLAYER
                ? creditorTarget(activeDebt.creditorPlayerId())
                : BankTarget.INSTANCE;
        if (target == null) {
            return false;
        }
        restoreDebtState(
                new PaymentRequest(debtor, target, activeDebt.amountRemaining(), activeDebt.reason()),
                onResolved,
                activeDebt.bankruptcyRisk()
        );
        return true;
    }

    private Player playerById(String playerId) {
        if (playerId == null) {
            return null;
        }
        return players.getPlayers().stream()
                .filter(p -> playerId.equals("player-" + p.getId()))
                .findFirst()
                .orElse(null);
    }

    private PaymentTarget creditorTarget(String creditorPlayerId) {
        Player creditor = playerById(creditorPlayerId);
        return creditor == null ? null : new PlayerTarget(creditor);
    }

    public void restoreDebtState(PaymentRequest request, Runnable onResolved, boolean bankruptcyRisk) {
        if (request == null) {
            debtState = null;
            onStateChanged.run();
            return;
        }
        debtState = new DebtState(request, onResolved, bankruptcyRisk);
        hidePrimaryTurnControls.run();
        onStateChanged.run();
        log.info("Restored debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), bankruptcyRisk);
    }

    public void retryPendingDebtPayment() {
        if (debtState == null) {
            return;
        }

        log.debug("Retrying pending debt payment for debtor={} amount={}",
                debtState.paymentRequest().debtor().getName(), debtState.paymentRequest().amount());
        PaymentResult result = paymentResolver.tryPay(debtState.paymentRequest());
        if (result.status() == PaymentStatus.PAID) {
            DebtState resolvedDebt = debtState;
            debtState = null;
            onStateChanged.run();
            log.info("Debt resolved for {}", resolvedDebt.paymentRequest().debtor().getName());
            runtime.popupService().show(text("game.debt.paid", resolvedDebt.paymentRequest().reason()), () -> resolvedDebt.onResolved().run());
            return;
        }

        debtState = new DebtState(debtState.paymentRequest(), debtState.onResolved(), result.status() == PaymentStatus.BANKRUPT);
        onStateChanged.run();
        log.info("Debt still unresolved for {}. bankruptcyRisk={}",
                debtState.paymentRequest().debtor().getName(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
    }

    public void declareBankruptcy() {
        if (debtState == null) {
            return;
        }
        if (!debtState.bankruptcyRisk()) {
            runtime.popupService().show(text("game.debt.assetsCover"));
            return;
        }
        PaymentRequest request = debtState.paymentRequest();
        log.warn("Declaring bankruptcy: debtor={}, target={}, amount={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount());
        int liquidationCash = request.debtor().liquidateBuildingsToBank();
        if (liquidationCash > 0) {
            log.info("Bankruptcy liquidation sold buildings for debtor={} and raised M{}",
                    request.debtor().getName(), liquidationCash);
        }
        if (request.target() instanceof PlayerTarget playerTarget) {
            request.debtor().transferAssetsTo(playerTarget.player());
            finishBankruptcyFlow(request);
        } else {
            List<Property> bankAuctionProperties = List.copyOf(request.debtor().getOwnedProperties());
            request.debtor().releaseAssetsToBank();
            finishBankruptcyFlowAfterBankRelease(request, bankAuctionProperties);
        }
    }

    private void finishBankruptcyFlowAfterBankRelease(PaymentRequest request, List<Property> bankAuctionProperties) {
        log.info("Bankruptcy returned {} debtor properties to bank for auction", bankAuctionProperties.size());
        removeBankruptPlayerFromGame(request);
        new PropertyAuctionResolver(runtime.popupService(), players).resolveAll(bankAuctionProperties, () -> completeBankruptcyFlow(request));
    }

    private void finishBankruptcyFlow(PaymentRequest request) {
        removeBankruptPlayerFromGame(request);
        completeBankruptcyFlow(request);
    }

    private void removeBankruptPlayerFromGame(PaymentRequest request) {
        request.debtor().setGetOutOfJailCardCount(0);
        players.removePlayer(request.debtor());
        debtState = null;
        onStateChanged.run();
    }

    private void completeBankruptcyFlow(PaymentRequest request) {
        if (players.count() <= 1) {
            Player lastPlayer = players.getPlayers().stream().findFirst().orElse(null);
            declareWinner.accept(lastPlayer != null ? "player-" + lastPlayer.getId() : null);
            return;
        }

        players.switchTurn();
        showRollDiceControl.run();
        log.info("Bankruptcy handled. Next turn player={}", players.getTurn() != null ? players.getTurn().getName() : "none");
        runtime.popupService().show(text("game.bankruptcy.playerWent", request.debtor().getName()));
    }

    private String buildDebtMessage(PaymentResult result) {
        if (debtState == null) {
            return text("game.debt.couldNotComplete");
        }
        return buildDebtMessage(debtState.paymentRequest(), result.missingAmount(), result.status() == PaymentStatus.BANKRUPT);
    }

    private String buildDebtMessage(PaymentRequest request, int missingAmount, boolean bankruptcyRisk) {
        StringBuilder text = new StringBuilder(text(
                "game.debt.message",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.reason(),
                request.debtor().getMoneyAmount(),
                missingAmount
        ));
        if (bankruptcyRisk) {
            text.append(text("game.debt.message.bankruptcyLine"));
        }
        return text.toString();
    }
}
