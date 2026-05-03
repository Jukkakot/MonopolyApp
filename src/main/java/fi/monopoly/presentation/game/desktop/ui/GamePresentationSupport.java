package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.host.bot.BotTurnScheduler;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

@RequiredArgsConstructor
public final class GamePresentationSupport {
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final GameUiController gameUiController;
    private final AuctionViewAdapter auctionViewAdapter;
    private final PendingDecisionPopupAdapter pendingDecisionPopupAdapter;
    private final TradeViewAdapter tradeViewAdapter;

    public void updateDebtButtons(DebtState debtState, SessionState sessionState) {
        var activeDebt = sessionState != null ? sessionState.activeDebt() : null;
        if (debtState == null || activeDebt == null) {
            retryDebtButton.hide();
            declareBankruptcyButton.hide();
            return;
        }
        retryDebtButton.show();
        if (activeDebt.bankruptcyRisk()) {
            declareBankruptcyButton.show();
        } else {
            declareBankruptcyButton.hide();
        }
    }

    public void updatePersistentButtons(boolean gameOver) {
        gameUiController.updatePersistentButtons(gameOver);
    }

    public void refreshLabels(boolean paused, BotTurnScheduler.SpeedMode botSpeedMode) {
        gameUiController.refreshLabels(paused, botSpeedMode);
    }

    public void updateLogTurnContext(boolean gameOver, String winnerName, String turnPlayerName) {
        if (gameOver && winnerName != null) {
            MDC.put("turnPlayer", winnerName);
            return;
        }
        MDC.put("turnPlayer", turnPlayerName != null ? turnPlayerName : "none");
    }

    public void syncTransientPresentationState() {
        pendingDecisionPopupAdapter.sync();
        auctionViewAdapter.sync();
        tradeViewAdapter.sync();
    }
}
