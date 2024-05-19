package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.types.SpotType;
import lombok.ToString;

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
