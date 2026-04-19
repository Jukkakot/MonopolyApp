package fi.monopoly.application.session.debt;

import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.properties.Property;

public interface DebtRemediationGateway {
    DebtState activeDebtState();

    Property propertyById(String propertyId);

    boolean mortgageProperty(String propertyId);

    boolean sellBuildings(String propertyId, int count);

    boolean sellBuildingRoundsAcrossSet(String propertyId, int rounds);

    void payDebtNow();

    void declareBankruptcy();
}
