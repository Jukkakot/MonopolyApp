package org.example.components.spots;

import lombok.Getter;
import lombok.Setter;
import org.example.components.Player;
import org.example.images.SpotImage;

import java.util.Arrays;
import java.util.List;

public abstract class PropertySpot extends Spot {
    @Getter
    protected int price;
    @Getter
    @Setter
    protected boolean isMortgaged = false;
    @Getter
    @Setter
    protected Player ownerPlayer;
    protected List<Integer> rentPrices;

    public PropertySpot(SpotImage sp) {
        super(sp);
        price = Integer.parseInt(spotType.getProperty("price"));
        String rentStr = spotType.getProperty("rents");
        if (rentStr != null && !rentStr.equals("")) {
            rentPrices = Arrays.stream(rentStr.split(",")).map(Integer::valueOf).toList();
        }
    }

    public boolean hasOwner() {
        return ownerPlayer != null;
    }

    public boolean isOwner(Player p) {
        return hasOwner() && ownerPlayer.equals(p);
    }

    public boolean payRent(Player player) {
        return ownerPlayer.giveMoney(player, getRent(player));
    }

    public abstract Integer getRent(Player player);
}
