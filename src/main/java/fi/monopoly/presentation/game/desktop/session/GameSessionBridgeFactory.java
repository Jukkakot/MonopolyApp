package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.trade.TradeOfferEvaluator;
import fi.monopoly.components.trade.TradeUiBuilder;
import fi.monopoly.presentation.legacy.session.LegacySessionApplicationFactory;
import fi.monopoly.presentation.legacy.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;

import java.util.List;

/**
 * Builds the desktop session bridge objects that sit between the legacy runtime and the
 * authoritative session application service.
 */
public final class GameSessionBridgeFactory {
    private final MonopolyRuntime runtime;

    public GameSessionBridgeFactory(MonopolyRuntime runtime) {
        this.runtime = runtime;
    }

    public GameSessionBridge create(
            String sessionId,
            Players players,
            Dices dices,
            DebtController debtController,
            Hooks hooks
    ) {
        SessionApplicationService sessionApplicationService = new LegacySessionApplicationFactory(
                sessionId,
                () -> players,
                () -> LegacyPopupSnapshot.fromPopupService(runtime.popupService()),
                () -> debtController != null ? debtController.debtState() : null,
                hooks::paused,
                hooks::gameOver,
                hooks::winner,
                hooks::projectedRollDiceActionAvailable,
                hooks::projectedEndTurnActionAvailable
        ).create(
                runtime.popupService(),
                debtController,
                dices,
                hooks::endTurn
        );

        DebtActionDispatcher debtActionDispatcher = new DebtActionDispatcher(
                sessionId,
                sessionApplicationService,
                runtime.popupService(),
                hooks::currentTurnPlayer
        );
        AuctionViewAdapter auctionViewAdapter = new AuctionViewAdapter(
                sessionId,
                sessionApplicationService,
                runtime.popupService(),
                players
        );
        LegacyTradeGateway legacyTradeGateway = new LegacyTradeGateway(hooks::players);
        TradeViewAdapter tradeViewAdapter = new TradeViewAdapter(
                sessionId,
                sessionApplicationService,
                runtime.popupService(),
                legacyTradeGateway,
                new TradeUiBuilder(new TradeOfferEvaluator()),
                hooks::computerTurn
        );
        PendingDecisionPopupAdapter pendingDecisionPopupAdapter = new PendingDecisionPopupAdapter(
                sessionId,
                sessionApplicationService,
                runtime.popupService(),
                sessionApplicationService::openPropertyPurchaseDecision,
                auctionViewAdapter::sync,
                hooks::playerById
        );
        TradeController tradeController = new TradeController(
                runtime,
                sessionId,
                sessionApplicationService,
                tradeViewAdapter,
                legacyTradeGateway,
                hooks::canOpenTrade,
                hooks::currentTurnPlayer,
                hooks::players
        );
        return new GameSessionBridge(
                sessionApplicationService,
                pendingDecisionPopupAdapter,
                debtActionDispatcher,
                auctionViewAdapter,
                tradeViewAdapter,
                tradeController
        );
    }

    public interface Hooks {
        boolean paused();

        boolean gameOver();

        Player winner();

        boolean projectedRollDiceActionAvailable();

        boolean projectedEndTurnActionAvailable();

        void endTurn();

        Player playerById(String playerId);

        boolean computerTurn();

        boolean canOpenTrade();

        Player currentTurnPlayer();

        List<Player> players();
    }

    public record GameSessionBridge(
            SessionApplicationService sessionApplicationService,
            PendingDecisionPopupAdapter pendingDecisionPopupAdapter,
            DebtActionDispatcher debtActionDispatcher,
            AuctionViewAdapter auctionViewAdapter,
            TradeViewAdapter tradeViewAdapter,
            TradeController tradeController
    ) {
    }
}
