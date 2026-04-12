package fi.monopoly.presentation.session.debt;

import fi.monopoly.application.command.DeclareBankruptcyCommand;
import fi.monopoly.application.command.MortgagePropertyForDebtCommand;
import fi.monopoly.application.command.PayDebtCommand;
import fi.monopoly.application.command.SellBuildingForDebtCommand;
import fi.monopoly.application.command.SellBuildingRoundsAcrossSetForDebtCommand;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.domain.session.DebtStateModel;
import fi.monopoly.types.SpotType;

import java.util.function.Supplier;

public final class DebtActionDispatcher {
    private final String sessionId;
    private final SessionApplicationService sessionApplicationService;
    private final PopupService popupService;
    private final Supplier<Player> actorSupplier;

    public DebtActionDispatcher(
            String sessionId,
            SessionApplicationService sessionApplicationService,
            PopupService popupService,
            Supplier<Player> actorSupplier
    ) {
        this.sessionId = sessionId;
        this.sessionApplicationService = sessionApplicationService;
        this.popupService = popupService;
        this.actorSupplier = actorSupplier;
    }

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
