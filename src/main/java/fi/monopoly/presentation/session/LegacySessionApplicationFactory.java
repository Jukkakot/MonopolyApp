package fi.monopoly.presentation.session;

import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.presentation.game.LegacyTurnActionGatewayAdapter;
import fi.monopoly.presentation.session.projection.LegacyPopupSnapshot;
import fi.monopoly.presentation.session.projection.LegacySessionProjector;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
        sessionApplicationService.configureRentAndDebtFlow(debtController);
        sessionApplicationService.configureAuctionFlow(popupService, playersSupplier.get());
        sessionApplicationService.configurePropertyPurchaseFlow(popupService, playersSupplier.get());
        sessionApplicationService.configureTradeFlow(() -> {
            Players players = playersSupplier.get();
            return players != null ? players.getPlayers() : List.of();
        });
        sessionApplicationService.configureTurnActionFlow(
                new LegacyTurnActionGatewayAdapter(
                        dices,
                        () -> {
                            Players players = playersSupplier.get();
                            return players != null ? players.getTurn() : null;
                        },
                        endTurnAction
                )
        );
        return sessionApplicationService;
    }
}
