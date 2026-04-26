package fi.monopoly.presentation.session.debt;

import fi.monopoly.application.command.*;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public final class DebtActionDispatcher {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;
    private final Supplier<Player> actorSupplier;

    public boolean payDebt() {
        DebtStateModel debt = activeDebt();
        Player actor = actorSupplier.get();
        if (debt == null || actor == null) {
            return false;
        }
        return handle(new PayDebtCommand(sessionId, playerId(actor), debt.debtId()));
    }

    public boolean declareBankruptcy() {
        DebtStateModel debt = activeDebt();
        Player actor = actorSupplier.get();
        if (debt == null || actor == null) {
            return false;
        }
        return handle(new DeclareBankruptcyCommand(sessionId, playerId(actor), debt.debtId()));
    }

    public boolean mortgageProperty(SpotType spotType) {
        DebtStateModel debt = activeDebt();
        Player actor = actorSupplier.get();
        if (debt == null || actor == null) {
            return false;
        }
        return handle(new MortgagePropertyForDebtCommand(sessionId, playerId(actor), debt.debtId(), spotType.name()));
    }

    public boolean sellBuilding(SpotType spotType, int count) {
        DebtStateModel debt = activeDebt();
        Player actor = actorSupplier.get();
        if (debt == null || actor == null) {
            return false;
        }
        return handle(new SellBuildingForDebtCommand(sessionId, playerId(actor), debt.debtId(), spotType.name(), count));
    }

    public boolean sellBuildingRoundsAcrossSet(SpotType spotType, int rounds) {
        DebtStateModel debt = activeDebt();
        Player actor = actorSupplier.get();
        if (debt == null || actor == null) {
            return false;
        }
        return handle(new SellBuildingRoundsAcrossSetForDebtCommand(sessionId, playerId(actor), debt.debtId(), spotType.name(), rounds));
    }

    private boolean handle(fi.monopoly.application.command.SessionCommand command) {
        var result = sessionApplicationService.handle(command);
        if (!result.accepted() && !result.rejections().isEmpty() && popupService != null) {
            popupService.show(result.rejections().get(0).message());
        }
        return result.accepted();
    }

    private DebtStateModel activeDebt() {
        return sessionApplicationService.currentState().activeDebt();
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }
}
