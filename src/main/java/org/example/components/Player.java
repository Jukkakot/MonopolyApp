package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.example.components.popup.OkPopup;
import org.example.components.properties.Properties;
import org.example.components.properties.Property;
import org.example.components.spots.JailSpot;
import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.types.StreetType;

import java.util.List;

@ToString
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
    private final Properties ownedProperties = new Properties();
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

    private void giveProperty(Property property) {
        property.setOwnerPlayer(this);
        if (!ownedProperties.addProperty(property)) {
            System.err.println("Player already owned property " + property);
        }
    }

    public boolean canBuyProperty(Property ps) {
        return ps.getOwnerPlayer() == null && money >= ps.getPrice();
    }

    public boolean buyProperty(Property property) {
        if (canBuyProperty(property)) {
            money -= property.getPrice();
            giveProperty(property);
            return true;
        }
        return false;
    }

    public boolean updateMoney(Integer amount) {
        if (money + amount >= 0) {
            money += amount;
            return true;
        }
        return false;
    }

    public boolean giveMoney(Player fromPlayer, Integer amount) {
        if (amount < 0) {
            System.out.println("Not possible to give negative amount of money.");
            return false;
        }
        if (fromPlayer.updateMoney(-amount)) {
            updateMoney(amount);
            return true;
        }
        OkPopup.showInfo(fromPlayer.getName() + " Didn't have enough money to give to " + name);
        return false;
    }

    public boolean ownsAllStreetProperties(StreetType streetType) {
        return SpotType.getNumberOfSpots(streetType).equals(getOwnedProperties(streetType).size());
    }

    public List<Property> getOwnedProperties(StreetType streetType) {
        return getOwnedProperties().stream().filter(property -> property.isSameStreetType(streetType)).toList();
    }

    public List<Property> getOwnedProperties() {
        return ownedProperties.getProperties();
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
        return ownedProperties.getHouseCount();
    }

    public int getHotelCount() {
        return ownedProperties.getHotelCount();
    }

    public boolean isInJail() {
        return JailSpot.playersRoundsLeftMap.get(this) != null;
    }
}
