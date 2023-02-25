package org.example.components.spots.propertySpots;

import org.example.components.Player;
import org.example.components.dices.Dices;
import org.example.images.SpotImage;

public class UtilityPropertySpot extends PropertySpot {
    public UtilityPropertySpot(SpotImage sp) {
        super(sp);
    }

    @Override
    public Integer getRent(Player player) {
        throw new RuntimeException("Trying to get rent without dice value from " + name);
    }

    public Integer getRent(Player player, Dices dices) {
        if (hasOwner() && !isOwner(player)) {
            int multiplier = 4;
            if (getOwnerPlayer().ownsAllSpots(spotType.streetType)) {
                multiplier = 10;
            }
            return dices.getValue().value() * multiplier;
        }
        return 0;

    }
}
