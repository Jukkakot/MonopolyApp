package org.example.components.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.components.Player;
import org.example.types.SpotType;
import org.example.types.StreetType;

import java.util.Arrays;
import java.util.List;

@ToString
public abstract class Property {

    @Getter
    protected int price;
    @Getter
    @Setter
    protected boolean isMortgaged = false;
    @Getter
    @Setter
    protected Player ownerPlayer;
    @Getter
    protected List<Integer> rentPrices;
    @Getter
    protected SpotType spotType;
    public static final int MOVE_NEAREST_CARD_MULTIPLIER = 2;

    public Property(SpotType spotType) {
        this.spotType = spotType;
        price = spotType.getIntegerProperty("price");
        String rentStr = spotType.getStringProperty("rents");
        if (rentStr.contains(",")) {
            rentPrices = Arrays.stream(rentStr.split(",")).map(Integer::valueOf).toList();
        }
    }

    public boolean hasOwner() {
        return ownerPlayer != null;
    }

    public boolean isOwner(Player p) {
        return hasOwner() && ownerPlayer.equals(p);
    }

    public boolean isNotOwner(Player p) {
        return !isOwner(p);
    }

    public boolean payRent(Player fromPlayer, Integer rent) {
        if (!hasOwner()) {
            throw new RuntimeException("Property has no owner to pay rent to");
        }
        return ownerPlayer.giveMoney(fromPlayer, rent);
    }

    public boolean isSameStreetType(StreetType streetType) {
        return spotType.streetType.equals(streetType);
    }

    public abstract Integer getRent(Player player);
}
