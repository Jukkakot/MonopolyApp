package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import org.example.components.popup.OkPopup;
import org.example.components.spots.JailSpot;
import org.example.components.spots.propertySpots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.images.SpotImage;
import org.example.types.SpotType;
import org.example.types.StreetType;

import java.util.List;

public class Player extends PlayerToken {
    private static int NEXT_ID = 0;
    @Getter
    private final int id;
    private final String name;
    @Getter
    private Integer money;
    @Getter
    private int turnNumber;
    @Getter
    private final Deeds deeds;
    @Getter
    @Setter
    private int getOutOfJailCardCount = 0;

    public Player(String name, Color color, Spot spot) {
        super(spot, color);
        this.id = NEXT_ID++;
        this.name = name;
        this.money = 1500;
        // turn number is id by default. Later maybe implement so that this can change
        this.turnNumber = id + 1; // Turn numbers starts from 1
        deeds = new Deeds();
        setSpot(spot);
    }

    public String getName() {
        return name.replace("ö", "o")
                .replace("ä", "a")
                .replace("Ö", "O")
                .replace("Ä", "A");
    }

    public void setSpot(Spot newSpot) {
        spot.removePlayer(this);
        spot = newSpot;
        spot.addPlayer(this);
    }

    private void addDeed(PropertySpot ps) {
        ps.setOwnerPlayer(this);
        deeds.addDeed(ps);
    }

    public boolean canBuyProperty(PropertySpot ps) {
        return ps.getOwnerPlayer() == null && money >= ps.getPrice();
    }

    public boolean buyProperty(PropertySpot ps) {
        if (canBuyProperty(ps)) {
            money -= ps.getPrice();
            addDeed(ps);
            return true;
        }
        return false;
    }

    public List<SpotImage> getAllDeeds() {
        return deeds.getAllDeeds();
    }

    public boolean updateMoney(Integer amount) {
        if (money + amount >= 0) {
            money += amount;
            return true;
        }
        return false;
    }

    public boolean giveMoney(Player from, Integer amount) {
        if (amount < 0) {
            System.out.println("Not possible to give negative amount of money.");
            return false;
        }
        if (from.updateMoney(-amount)) {
            updateMoney(amount);
            return true;
        }
        OkPopup.showInfo(from.getName() + " Didn't have enough money to give to " + name);
        return false;
    }

    public List<SpotImage> getOwnedSpots(StreetType streetType) {
        return deeds.getDeeds(streetType);
    }

    public boolean ownsAllSpots(StreetType streetType) {
        return SpotType.getNumberOfSpots(streetType).equals(getOwnedSpots(streetType).size());
    }

    public void addOutOfJailCard() {
        getOutOfJailCardCount++;
    }

    public boolean hasGetOutOfJailCard() {
        return getOutOfJailCardCount > 0;
    }

    public boolean useGetOutOfJailCard() {
        if (hasGetOutOfJailCard()) {
            getOutOfJailCardCount--;
            return true;
        }
        return false;
    }

    public int getHouseCount() {
        return deeds.getHouseCount();
    }

    public int getHotelCount() {
        return deeds.getHotelCount();
    }

    public boolean isInJail() {
        return JailSpot.playersRoundsLeftMap.get(this) != null;
    }
}
