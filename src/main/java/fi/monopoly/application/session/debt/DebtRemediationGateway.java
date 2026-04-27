package fi.monopoly.application.session.debt;

import fi.monopoly.components.payment.DebtState;

public interface DebtRemediationGateway {
    DebtState activeDebtState();

    boolean canMortgage(String propertyId, String debtorPlayerId);

    boolean mortgageProperty(String propertyId);

    boolean canSellBuildings(String propertyId, int count, String debtorPlayerId);

    boolean sellBuildings(String propertyId, int count);

    boolean canSellBuildingRoundsAcrossSet(String propertyId, int rounds, String debtorPlayerId);

    boolean sellBuildingRoundsAcrossSet(String propertyId, int rounds);

    void payDebtNow();

    void declareBankruptcy();
}
