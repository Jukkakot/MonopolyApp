package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.client.session.SessionPaymentPort;
import fi.monopoly.application.session.SessionPresentationStatePort;
import fi.monopoly.application.session.turn.TurnContinuationGateway;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.client.session.ForwardingSessionCommandPort;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.trade.TradeOfferEvaluator;
import fi.monopoly.components.trade.TradeUiBuilder;
import fi.monopoly.presentation.legacy.session.LegacySessionApplicationFactory;
import fi.monopoly.presentation.legacy.session.LegacySessionPaymentPort;
import fi.monopoly.presentation.legacy.session.debt.LegacyPaymentGateway;
import fi.monopoly.presentation.legacy.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;
import fi.monopoly.presentation.session.auction.AuctionViewAdapter;
import fi.monopoly.presentation.session.debt.DebtActionDispatcher;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.purchase.PendingDecisionPopupAdapter;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.presentation.session.trade.TradeViewAdapter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds the desktop session bridge objects that sit between the legacy runtime and the
 * authoritative session application service.
 */
@RequiredArgsConstructor
public final class GameSessionBridgeFactory {
    private final MonopolyRuntime runtime;

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
                hooks::winnerPlayerId,
                hooks::projectedRollDiceActionAvailable,
                hooks::projectedEndTurnActionAvailable
        ).create(
                runtime.popupService(),
                debtController,
                dices,
                hooks::endTurn
        );

        LegacySessionPaymentPort legacySessionPaymentPort = new LegacySessionPaymentPort(
                sessionApplicationService, new LegacyPaymentGateway(debtController));

        // Proxy for adapters: routes handle() through EmbeddedDesktopSessionHost once wired,
        // while currentState() always returns the locally-projected state.
        ForwardingSessionCommandPort commandProxy = new ForwardingSessionCommandPort(sessionApplicationService);

        DebtActionDispatcher debtActionDispatcher = new DebtActionDispatcher(
                sessionId,
                commandProxy,
                runtime.popupService()
        );
        AuctionViewAdapter auctionViewAdapter = new AuctionViewAdapter(
                sessionId,
                commandProxy,
                runtime.popupService(),
                players
        );
        LegacyTradeGateway legacyTradeGateway = new LegacyTradeGateway(hooks::players);
        TradeViewAdapter tradeViewAdapter = new TradeViewAdapter(
                sessionId,
                commandProxy,
                runtime.popupService(),
                legacyTradeGateway,
                new TradeUiBuilder(new TradeOfferEvaluator()),
                hooks::computerTurn
        );
        PendingDecisionPopupAdapter pendingDecisionPopupAdapter = new PendingDecisionPopupAdapter(
                sessionId,
                commandProxy,
                runtime.popupService(),
                sessionApplicationService::openPropertyPurchaseDecision,
                auctionViewAdapter::sync
        );
        TradeController tradeController = new TradeController(
                runtime,
                sessionId,
                commandProxy,
                tradeViewAdapter,
                legacyTradeGateway,
                hooks::canOpenTrade,
                hooks::currentTurnPlayer,
                hooks::players
        );
        return new GameSessionBridge(
                commandProxy,
                sessionApplicationService,
                legacySessionPaymentPort,
                sessionApplicationService,
                sessionApplicationService::configureTurnContinuationFlow,
                sessionApplicationService::handleComputerAuctionAction,
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

        String winnerPlayerId();

        boolean projectedRollDiceActionAvailable();

        boolean projectedEndTurnActionAvailable();

        void endTurn();

        boolean computerTurn();

        boolean canOpenTrade();

        Player currentTurnPlayer();

        List<Player> players();
    }

    public record GameSessionBridge(
            SessionCommandPort sessionCommandPort,
            SessionPresentationStatePort sessionPresentationStatePort,
            SessionPaymentPort sessionPaymentPort,
            SessionCommandPort internalCommandPort,
            Consumer<TurnContinuationGateway> turnContinuationConfigurator,
            Function<String, CommandResult> computerAuctionActionHandler,
            PendingDecisionPopupAdapter pendingDecisionPopupAdapter,
            DebtActionDispatcher debtActionDispatcher,
            AuctionViewAdapter auctionViewAdapter,
            TradeViewAdapter tradeViewAdapter,
            TradeController tradeController
    ) {
    }
}
