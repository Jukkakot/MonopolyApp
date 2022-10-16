package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.images.SpotImage;

public class StreetPropertySpot extends PropertySpot {
    private final int housePrice;
    private int houseCount;
    private boolean isMortgaged = false;

    public StreetPropertySpot(MonopolyApp p, SpotImage spotImage) {
        super(p, spotImage);
        housePrice = Integer.parseInt(spotTypeEnum.getProperty("housePrice"));
    }
}
