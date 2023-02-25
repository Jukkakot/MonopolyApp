package org.example.components.spots.propertySpots;

import org.example.components.Player;
import org.example.images.SpotImage;

public class RailRoadPropertySpot extends PropertySpot {
    public RailRoadPropertySpot(SpotImage sp) {
        super(sp);
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
