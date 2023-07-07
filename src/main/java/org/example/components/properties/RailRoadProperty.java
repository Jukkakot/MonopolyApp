package org.example.components.properties;

import lombok.ToString;
import org.example.components.Player;
import org.example.types.SpotType;

@ToString(callSuper = true)
public class RailRoadProperty extends Property {
    public RailRoadProperty(SpotType spotType) {
        super(spotType);
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            int ownedRailRoadsCount = getOwnerPlayer().getOwnedProperties(spotType.streetType).size();
            return getRentPrices().get(ownedRailRoadsCount);
        }
        return 0;
    }
}
