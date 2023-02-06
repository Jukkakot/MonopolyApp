package org.example.components.spots;

import lombok.Getter;
import lombok.Setter;
import org.example.MonopolyApp;
import org.example.components.Player;
import org.example.images.SpotImage;

public class PropertySpot extends Spot implements SpotInterface {
    @Getter
    protected int price;
    protected int defaultRent;
    @Getter
    @Setter
    protected boolean isMortgaged = false;
    @Getter
    @Setter
    protected Player ownerPlayer;

    public PropertySpot(MonopolyApp p, SpotImage sp) {
        super(p, sp);
        price = Integer.parseInt(spotTypeEnum.getProperty("price"));
    }

    protected boolean isOwner(Player p) {
        return hasOwner() && ownerPlayer.equals(p);
    }

    protected boolean hasOwner() {
        return ownerPlayer != null;
    }

    @Override
    public String getPopupText(Player p) {
        if (!hasOwner()) {
            return "Arrived at " + name + " do you want to buy it?";
        } else if (isOwner(p)) {
            return null;
//            return "You own " + name + " Welcome!";
        } else {
            return "Uh oh... you need to pay M" + price + " rent to " + ownerPlayer.getName();
        }
    }
}
