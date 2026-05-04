package fi.monopoly.presentation.legacy.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.presentation.game.desktop.session.LegacyTurnActionGatewayAdapter;
import fi.monopoly.presentation.legacy.session.auction.LegacyAuctionGateway;
import fi.monopoly.presentation.legacy.session.debt.LegacyDebtRemediationGateway;
import fi.monopoly.presentation.legacy.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.legacy.session.projection.LegacySessionProjector;
import fi.monopoly.presentation.legacy.session.purchase.LegacyPropertyPurchaseGateway;
import fi.monopoly.presentation.legacy.session.trade.LegacyTradeGateway;
import fi.monopoly.presentation.session.debt.DebtController;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public final class LegacySessionApplicationFactory {
    private final String sessionId;
    private final Supplier<Players> playersSupplier;
    private final Supplier<LegacyPopupSnapshot> popupSnapshotSupplier;
    private final Supplier<DebtState> debtStateSupplier;
    private final BooleanSupplier pausedSupplier;
    private final BooleanSupplier gameOverSupplier;
    private final Supplier<String> winnerPlayerIdSupplier;
    private final BooleanSupplier canRollSupplier;
    private final BooleanSupplier canEndTurnSupplier;


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
                        winnerPlayerIdSupplier,
                        canRollSupplier,
                        canEndTurnSupplier
                )::project
        );
        Players players = playersSupplier.get();
        sessionApplicationService.configureDebtRemediationFlow(new LegacyDebtRemediationGateway(debtController));
        sessionApplicationService.configureAuctionFlow(new LegacyAuctionGateway(popupService, players));
        sessionApplicationService.configurePropertyPurchaseFlow(new LegacyPropertyPurchaseGateway(players));
        sessionApplicationService.configureTradeFlow(new LegacyTradeGateway(() -> {
            Players currentPlayers = playersSupplier.get();
            return currentPlayers != null ? currentPlayers.getPlayers() : List.of();
        }));
        sessionApplicationService.configureTurnActionFlow(
                new LegacyTurnActionGatewayAdapter(
                        dices,
                        () -> {
                            Players currentPlayers = playersSupplier.get();
                            return currentPlayers != null && currentPlayers.getTurn() != null;
                        },
                        endTurnAction
                )
        );
        return sessionApplicationService;
    }
}
