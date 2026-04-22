package fi.monopoly.components.properties;

import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.types.SpotType;
import lombok.ToString;

@ToString(callSuper = true)
public class UtilityProperty extends Property {
    private static final int MOVE_NEAREST_CARD_UTIL_MULTIPLIER = 10;

    public UtilityProperty(SpotType spotType) {
        super(spotType);
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            MonopolyRuntime runtime = ownerPlayer != null ? ownerPlayer.getRuntime() : null;
            if (runtime == null || runtime.gameSessionOrNull() == null || runtime.gameSessionOrNull().dices() == null
                    || runtime.gameSessionOrNull().dices().getValue() == null) {
                return 0;
            }
            return getRentForDiceValue(player, runtime.gameSessionOrNull().dices().getValue().value());
        }
        return 0;
    }

    public Integer getMultiplierRent(Dices dices) {
        return getMultiplierRent(dices.getValue().value());
    }

    public Integer getRentForDiceValue(Player player, int diceValue) {
        if (hasOwner() && isNotOwner(player)) {
            int multiplier = 4;
            if (getOwnerPlayer().ownsAllStreetProperties(spotType.streetType)) {
                multiplier = 10;
            }
            return diceValue * multiplier;
        }
        return 0;
    }

    public Integer getMultiplierRent(int diceValue) {
        return diceValue * MOVE_NEAREST_CARD_UTIL_MULTIPLIER;
    }
}
