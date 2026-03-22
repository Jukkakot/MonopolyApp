package fi.monopoly.components.turn;

import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.UtilityProperty;
import fi.monopoly.types.StreetType;
import fi.monopoly.types.TurnResult;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

public class PropertyTurnResolver {

    public List<TurnEffect> resolve(GameState gameState, String spotName, Property property) {
        Player turnPlayer = gameState.getPlayers().getTurn();
        List<TurnEffect> effects = new ArrayList<>();

        if (!property.hasOwner()) {
            effects.add(new OfferToBuyPropertyEffect(
                    turnPlayer,
                    property,
                    text("property.offerToBuy", spotName, property.getPrice())
            ));
            return effects;
        }

        if (property.isNotOwner(turnPlayer) && !property.isMortgaged()) {
            int rent = calculateRent(gameState, turnPlayer, property);
            effects.add(new PayRentEffect(turnPlayer, property.getOwnerPlayer(), rent,
                    text("property.payRent", rent, property.getOwnerPlayer().getName())));
        }

        return effects;
    }

    private int calculateRent(GameState gameState, Player turnPlayer, Property property) {
        TurnResult prevTurnResult = gameState.getPrevTurnResult();
        boolean wasMoveNearestCardLast = prevTurnResult != null && prevTurnResult.getNextSpotCriteria() instanceof StreetType;
        if (!wasMoveNearestCardLast) {
            return property.getRent(turnPlayer);
        }
        if (property instanceof UtilityProperty utilityProperty) {
            return utilityProperty.getMultiplierRent(gameState.getDices().getValue().value());
        }
        return Property.MOVE_NEAREST_CARD_MULTIPLIER * property.getRent(turnPlayer);
    }
}
