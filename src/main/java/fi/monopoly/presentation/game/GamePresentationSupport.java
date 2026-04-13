package fi.monopoly.presentation.game;

import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import org.slf4j.MDC;

public final class GamePresentationSupport {
    private final MonopolyButton retryDebtButton;
    private final MonopolyButton declareBankruptcyButton;
    private final GameUiController gameUiController;
    private final AuctionViewAdapter auctionViewAdapter;
    private final TradeViewAdapter tradeViewAdapter;

    public GamePresentationSupport(
            MonopolyButton retryDebtButton,
            MonopolyButton declareBankruptcyButton,
            GameUiController gameUiController,
            AuctionViewAdapter auctionViewAdapter,
            TradeViewAdapter tradeViewAdapter
    ) {
        this.retryDebtButton = retryDebtButton;
        this.declareBankruptcyButton = declareBankruptcyButton;
        this.gameUiController = gameUiController;
        this.auctionViewAdapter = auctionViewAdapter;
        this.tradeViewAdapter = tradeViewAdapter;
    }

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

    public void updateLogTurnContext(boolean gameOver, Player winner, Player turnPlayer) {
        if (gameOver && winner != null) {
            MDC.put("turnPlayer", winner.getName());
            return;
        }
        MDC.put("turnPlayer", turnPlayer != null ? turnPlayer.getName() : "none");
    }

    public void syncTransientPresentationState() {
        auctionViewAdapter.sync();
        tradeViewAdapter.sync();
    }
}
