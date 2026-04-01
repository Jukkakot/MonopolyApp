package fi.monopoly.components.turn;

import fi.monopoly.components.GameState;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.RailRoadProperty;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.properties.UtilityProperty;
import fi.monopoly.components.dices.DiceValue;
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
                    buildRentMessage(gameState, property, rent)));
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

    private String buildRentMessage(GameState gameState, Property property, int rent) {
        return text(
                "property.payRent",
                rent,
                property.getOwnerPlayer().getName(),
                property.getDisplayName(),
                describeRentSource(gameState, property)
        );
    }

    private String describeRentSource(GameState gameState, Property property) {
        if (property instanceof StreetProperty streetProperty) {
            if (streetProperty.getHotelCount() > 0) {
                return text("property.payRent.details.street.hotel", streetProperty.getHotelCount());
            }
            if (streetProperty.getHouseCount() > 0) {
                return text("property.payRent.details.street.house", streetProperty.getHouseCount());
            }
            boolean monopoly = property.getOwnerPlayer() != null
                    && property.getOwnerPlayer().ownsAllStreetProperties(property.getSpotType().streetType);
            return monopoly
                    ? text("property.payRent.details.street.monopoly")
                    : text("property.payRent.details.street.base");
        }
        if (property instanceof RailRoadProperty) {
            int ownedRailroads = property.getOwnerPlayer() == null
                    ? 0
                    : property.getOwnerPlayer().getOwnedProperties(StreetType.RAILROAD).size();
            return text("property.payRent.details.railroad", ownedRailroads);
        }
        if (property instanceof UtilityProperty utilityProperty) {
            int diceValue = resolveDiceValue(gameState);
            int multiplier = diceValue <= 0 ? 0 : utilityProperty.getRentForDiceValue(gameState.getPlayers().getTurn(), diceValue) / diceValue;
            return text("property.payRent.details.utility", diceValue, multiplier);
        }
        return text("property.payRent.details.base");
    }

    private int resolveDiceValue(GameState gameState) {
        if (gameState == null || gameState.getDices() == null || gameState.getDices().getValue() == null) {
            return 0;
        }
        DiceValue diceValue = gameState.getDices().getValue();
        return Math.max(0, diceValue.value());
    }
}
