package org.example.components.spots;

import org.example.components.Player;
import org.example.images.SpotImage;

public class StreetPropertySpot extends PropertySpot {
    private final int housePrice;
    private int houseCount = 0;

    public StreetPropertySpot(SpotImage spotImage) {
        super(spotImage);
        housePrice = Integer.parseInt(spotType.getProperty("housePrice"));
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && !isOwner(player)) {
            Integer rentAmount = rentPrices.get(houseCount);
            if (getOwnerPlayer().ownsAllSpots(spotType.streetType)) {
                return rentAmount * 2;
            } else {
                return rentAmount;
            }
        }
        return 0;
    }
}
