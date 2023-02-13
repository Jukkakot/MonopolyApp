package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.components.Player;
import org.example.images.SpotImage;

public class RailRoadPropertySpot extends PropertySpot {
    public RailRoadPropertySpot(MonopolyApp p, SpotImage sp) {
        super(p, sp);
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && !isOwner(player)) {
            int ownedRailRoadsCount = getOwnerPlayer().getOwnedSpots(spotType.streetType).size();
            return rentPrices.get(ownedRailRoadsCount);
        }
        return 0;
    }
}
