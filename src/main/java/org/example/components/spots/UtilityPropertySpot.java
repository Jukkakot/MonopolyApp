package org.example.components.spots;

import org.example.components.dices.Dices;
import org.example.components.Player;
import org.example.images.SpotImage;

public class UtilityPropertySpot extends PropertySpot {
    public UtilityPropertySpot(SpotImage sp) {
        super(sp);
    }

    @Override
    public Integer getRent(Player player) {
        System.err.println("Trying to get rent without dice value from " + getName());
        return null;
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
    public boolean payRent(Player player) {
        return ownerPlayer.giveMoney(player, getRent(player));
    }
}
