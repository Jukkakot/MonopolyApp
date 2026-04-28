package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.application.session.turn.TurnActionGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
public final class LegacyTurnActionGatewayAdapter implements TurnActionGateway {
    private final Dices dices;
    private final Supplier<Player> turnPlayerSupplier;
    private final Runnable endTurnAction;

    @Override
    public boolean rollDice() {
        if (dices == null) {
            return false;
        }
        dices.rollDice();
        return true;
    }

    @Override
    public boolean endTurn() {
        if (turnPlayerSupplier.get() == null) {
            return false;
        }
        endTurnAction.run();
        return true;
    }

    @Override
    public boolean buyBuildingRound(String propertyId) {
        Property property = propertyById(propertyId);
        return property instanceof StreetProperty streetProperty
                && streetProperty.buyBuildingRoundsAcrossSet(1);
    }

    @Override
    public boolean toggleMortgage(String propertyId) {
        Property property = propertyById(propertyId);
        return property != null && property.handleMortgaging();
    }

    private Property propertyById(String propertyId) {
        try {
            return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
