package org.example.components.properties;

import lombok.Getter;
import lombok.ToString;
import org.example.components.Player;
import org.example.types.SpotType;

@ToString(callSuper = true)
public class StreetProperty extends Property {
    private final int housePrice;
    @Getter
    private int houseCount = 0, hotelCount = 0;

    public StreetProperty(SpotType spotType) {
        super(spotType);
        housePrice = spotType.getIntegerProperty("housePrice");
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            Integer rentAmount = getRentPrices().get(houseCount);
            if (getOwnerPlayer().ownsAllSpots(spotType.streetType)) {
                return rentAmount * 2;
            } else {
                return rentAmount;
            }
        }
        return 0;
    }
}
