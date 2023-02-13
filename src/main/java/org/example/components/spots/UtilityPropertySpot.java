package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.components.Dices;
import org.example.components.Player;
import org.example.images.SpotImage;

public class UtilityPropertySpot extends PropertySpot {
    public UtilityPropertySpot(MonopolyApp p, SpotImage sp) {
        super(p, sp);
    }

    @Override
    public Integer getRent(Player player) {
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
}
