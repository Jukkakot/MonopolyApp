package fi.monopoly.presentation.session.debt;

import fi.monopoly.application.command.*;
import fi.monopoly.client.session.SessionCommandPort;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class DebtActionDispatcher {
    private final String sessionId;
    private final SessionCommandPort sessionApplicationService;
    private final PopupService popupService;

    public boolean payDebt() {
        DebtStateModel debt = activeDebt();
        if (debt == null) {
            return false;
        }
        return handle(new PayDebtCommand(sessionId, debt.debtorPlayerId(), debt.debtId()));
    }

    public boolean declareBankruptcy() {
        DebtStateModel debt = activeDebt();
        if (debt == null) {
            return false;
        }
        return handle(new DeclareBankruptcyCommand(sessionId, debt.debtorPlayerId(), debt.debtId()));
    }

    public boolean mortgageProperty(SpotType spotType) {
        DebtStateModel debt = activeDebt();
        if (debt == null) {
            return false;
        }
        return handle(new MortgagePropertyForDebtCommand(sessionId, debt.debtorPlayerId(), debt.debtId(), spotType.name()));
    }

    public boolean sellBuilding(SpotType spotType, int count) {
        DebtStateModel debt = activeDebt();
        if (debt == null) {
            return false;
        }
        return handle(new SellBuildingForDebtCommand(sessionId, debt.debtorPlayerId(), debt.debtId(), spotType.name(), count));
    }

    public boolean sellBuildingRoundsAcrossSet(SpotType spotType, int rounds) {
        DebtStateModel debt = activeDebt();
        if (debt == null) {
            return false;
        }
        return handle(new SellBuildingRoundsAcrossSetForDebtCommand(sessionId, debt.debtorPlayerId(), debt.debtId(), spotType.name(), rounds));
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
}
