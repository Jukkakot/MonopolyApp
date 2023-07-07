package org.example.components.properties;

import lombok.ToString;
import org.example.components.Game;
import org.example.components.Player;
import org.example.components.dices.Dices;
import org.example.types.SpotType;

@ToString(callSuper = true)
public class UtilityProperty extends Property {
    private static final int MOVE_NEAREST_CARD_UTIL_MULTIPLIER = 10;

    public UtilityProperty(SpotType spotType) {
        super(spotType);
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            int multiplier = 4;
            if (getOwnerPlayer().ownsAllSpots(spotType.streetType)) {
                multiplier = 10;
            }
            return Game.DICES.getValue().value() * multiplier;
        }
        return 0;
    }

    public Integer getMultiplierRent(Dices dices) {
        return dices.getValue().value() * MOVE_NEAREST_CARD_UTIL_MULTIPLIER;
    }
}
