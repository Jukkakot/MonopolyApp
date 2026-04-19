package fi.monopoly.presentation.legacy.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.presentation.game.LegacyTurnActionGatewayAdapter;
import fi.monopoly.presentation.legacy.session.auction.LegacyAuctionGateway;
import fi.monopoly.presentation.legacy.session.debt.LegacyDebtRemediationGateway;
import fi.monopoly.presentation.legacy.session.debt.LegacyPaymentGateway;
import fi.monopoly.presentation.legacy.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.legacy.session.projection.LegacySessionProjector;
import fi.monopoly.presentation.legacy.session.purchase.LegacyPropertyPurchaseGateway;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Builds a {@link SessionApplicationService} wired against the current legacy desktop runtime.
 *
 * <p>This centralizes the temporary bridge code that still connects separated application flows
 * to Processing-era objects such as popups, players, debt controllers, and direct turn actions.
 * Keeping that wiring in one place makes the remaining backend extraction work easier to see and
 * replace incrementally.</p>
 */
public final class LegacySessionApplicationFactory {
    private final String sessionId;
    private final Supplier<Players> playersSupplier;
    private final Supplier<LegacyPopupSnapshot> popupSnapshotSupplier;
    private final Supplier<DebtState> debtStateSupplier;
    private final BooleanSupplier pausedSupplier;
    private final BooleanSupplier gameOverSupplier;
    private final Supplier<Player> winnerSupplier;
    private final BooleanSupplier canRollSupplier;
    private final BooleanSupplier canEndTurnSupplier;

    public LegacySessionApplicationFactory(
            String sessionId,
            Supplier<Players> playersSupplier,
            Supplier<LegacyPopupSnapshot> popupSnapshotSupplier,
            Supplier<DebtState> debtStateSupplier,
            BooleanSupplier pausedSupplier,
            BooleanSupplier gameOverSupplier,
            Supplier<Player> winnerSupplier,
            BooleanSupplier canRollSupplier,
            BooleanSupplier canEndTurnSupplier
    ) {
        this.sessionId = sessionId;
        this.playersSupplier = playersSupplier;
        this.popupSnapshotSupplier = popupSnapshotSupplier;
        this.debtStateSupplier = debtStateSupplier;
        this.pausedSupplier = pausedSupplier;
        this.gameOverSupplier = gameOverSupplier;
        this.winnerSupplier = winnerSupplier;
        this.canRollSupplier = canRollSupplier;
        this.canEndTurnSupplier = canEndTurnSupplier;
    }

    public SessionApplicationService create(
            PopupService popupService,
            DebtController debtController,
            Dices dices,
            Runnable endTurnAction
    ) {
        SessionApplicationService sessionApplicationService = new SessionApplicationService(
                sessionId,
                new LegacySessionProjector(
                        sessionId,
                        playersSupplier,
                        popupSnapshotSupplier,
                        debtStateSupplier,
                        pausedSupplier,
                        gameOverSupplier,
                        winnerSupplier,
                        canRollSupplier,
                        canEndTurnSupplier
                )::project
        );
        Players players = playersSupplier.get();
        sessionApplicationService.configureRentAndDebtFlow(
                new LegacyPaymentGateway(debtController),
                new LegacyDebtRemediationGateway(debtController)
        );
        sessionApplicationService.configureAuctionFlow(new LegacyAuctionGateway(popupService, players));
        sessionApplicationService.configurePropertyPurchaseFlow(new LegacyPropertyPurchaseGateway(popupService, players));
        sessionApplicationService.configureTradeFlow(new LegacyTradeGateway(() -> {
            Players currentPlayers = playersSupplier.get();
            return currentPlayers != null ? currentPlayers.getPlayers() : List.of();
        }));
        sessionApplicationService.configureTurnActionFlow(
                new LegacyTurnActionGatewayAdapter(
                        dices,
                        () -> {
                            Players currentPlayers = playersSupplier.get();
                            return currentPlayers != null ? currentPlayers.getTurn() : null;
                        },
                        endTurnAction
                )
        );
        return sessionApplicationService;
    }
}
