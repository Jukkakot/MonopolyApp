package fi.monopoly.components.payment;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.turn.PropertyAuctionResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.Consumer;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public final class DebtController {
    private final MonopolyRuntime runtime;
    private final Players players;
    private final PaymentResolver paymentResolver = new PaymentResolver();
    private final Runnable hidePrimaryTurnControls;
    private final Runnable showRollDiceControl;
    private final Runnable onStateChanged;
    private final Consumer<Player> declareWinner;

    private DebtState debtState;

    public DebtController(
            MonopolyRuntime runtime,
            Players players,
            Runnable hidePrimaryTurnControls,
            Runnable showRollDiceControl,
            Runnable onStateChanged,
            Consumer<Player> declareWinner
    ) {
        this.runtime = runtime;
        this.players = players;
        this.hidePrimaryTurnControls = hidePrimaryTurnControls;
        this.showRollDiceControl = showRollDiceControl;
        this.onStateChanged = onStateChanged;
        this.declareWinner = declareWinner;
    }

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

        debtState = new DebtState(request, onResolved, result.status() == PaymentStatus.BANKRUPT);
        hidePrimaryTurnControls.run();
        onStateChanged.run();
        log.info("Entering debt resolution: debtor={}, target={}, amount={}, bankruptcyRisk={}",
                request.debtor().getName(), request.target().getDisplayName(), request.amount(), debtState.bankruptcyRisk());
        runtime.popupService().show(buildDebtMessage(result));
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
            runtime.popupService().show(text("game.debt.paid", resolvedDebt.paymentRequest().reason()), resolvedDebt.onResolved()::doAction);
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
            declareWinner.accept(players.getPlayers().stream().findFirst().orElse(null));
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

        PaymentRequest request = debtState.paymentRequest();
        StringBuilder text = new StringBuilder(text(
                "game.debt.message",
                request.debtor().getName(),
                request.amount(),
                request.target().getDisplayName(),
                request.reason(),
                request.debtor().getMoneyAmount(),
                result.missingAmount()
        ));
        if (result.status() == PaymentStatus.BANKRUPT) {
            text.append(text("game.debt.message.bankruptcyLine"));
        }
        return text.toString();
    }
}
