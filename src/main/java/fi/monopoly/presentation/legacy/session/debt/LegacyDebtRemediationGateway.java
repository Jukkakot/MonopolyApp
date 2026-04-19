package fi.monopoly.presentation.legacy.session.debt;

import fi.monopoly.application.session.debt.DebtRemediationGateway;
import fi.monopoly.components.payment.DebtController;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.SpotType;

public class LegacyDebtRemediationGateway implements DebtRemediationGateway {
    private final DebtController debtController;

    public LegacyDebtRemediationGateway(DebtController debtController) {
        this.debtController = debtController;
    }

    public DebtState activeDebtState() {
        return debtController.debtState();
    }

    public Property propertyById(String propertyId) {
        return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
    }

    public boolean mortgageProperty(String propertyId) {
        return propertyById(propertyId).handleMortgaging();
    }

    public boolean sellBuildings(String propertyId, int count) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty && streetProperty.sellHouses(count);
    }

    public boolean sellBuildingRoundsAcrossSet(String propertyId, int rounds) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty && streetProperty.sellBuildingRoundsAcrossSet(rounds);
    }

    public void payDebtNow() {
        debtController.retryPendingDebtPayment();
    }

    public void declareBankruptcy() {
        debtController.declareBankruptcy();
    }
}
