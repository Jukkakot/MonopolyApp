package org.example.components.spots;

import org.example.MonopolyApp;
import org.example.Player;
import org.example.images.SpotImage;

public class PropertySpot extends Spot {
    protected int value;
    protected int defaultRent;
    protected Player ownerPlayer;
    public PropertySpot(MonopolyApp p, SpotImage sp) {
        super(p, sp);
        value = Integer.parseInt(spotTypeEnum.getProperty("price"));
    }
}
