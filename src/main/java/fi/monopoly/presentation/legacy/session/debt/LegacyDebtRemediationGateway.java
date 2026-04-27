package fi.monopoly.presentation.legacy.session.debt;

import fi.monopoly.application.session.debt.DebtRemediationGateway;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LegacyDebtRemediationGateway implements DebtRemediationGateway {
    private final DebtController debtController;

    @Override
    public DebtState activeDebtState() {
        return debtController.debtState();
    }

    @Override
    public boolean canMortgage(String propertyId, String debtorPlayerId) {
        Property property = propertyById(propertyId);
        if (property == null || property.isMortgaged() || !isOwnedByDebtor(property, debtorPlayerId)) {
            return false;
        }
        if (property instanceof StreetProperty streetProperty) {
            return streetProperty.getOwnerPlayer()
                    .getOwnedStreetProperties(property.getSpotType().streetType)
                    .stream()
                    .noneMatch(StreetProperty::hasBuildings);
        }
        return true;
    }

    @Override
    public boolean mortgageProperty(String propertyId) {
        Property property = propertyById(propertyId);
        return property != null && property.handleMortgaging();
    }

    @Override
    public boolean canSellBuildings(String propertyId, int count, String debtorPlayerId) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty
                && isOwnedByDebtor(property, debtorPlayerId)
                && streetProperty.canSellHouses(count);
    }

    @Override
    public boolean sellBuildings(String propertyId, int count) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty && streetProperty.sellHouses(count);
    }

    @Override
    public boolean canSellBuildingRoundsAcrossSet(String propertyId, int rounds, String debtorPlayerId) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty
                && isOwnedByDebtor(property, debtorPlayerId)
                && streetProperty.canSellBuildingRoundsAcrossSet(rounds);
    }

    @Override
    public boolean sellBuildingRoundsAcrossSet(String propertyId, int rounds) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty && streetProperty.sellBuildingRoundsAcrossSet(rounds);
    }

    @Override
    public void payDebtNow() {
        debtController.retryPendingDebtPayment();
    }

    @Override
    public void declareBankruptcy() {
        debtController.declareBankruptcy();
    }

    private Property propertyById(String propertyId) {
        if (propertyId == null) {
            return null;
        }
        try {
            return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isOwnedByDebtor(Property property, String debtorPlayerId) {
        return property.getOwnerPlayer() != null
                && ("player-" + property.getOwnerPlayer().getId()).equals(debtorPlayerId);
    }
}
