package org.example.components;

import javafx.scene.paint.Color;
import lombok.Getter;
import org.example.Drawable;
import org.example.components.spots.PropertySpot;
import org.example.components.spots.Spot;
import org.example.types.SpotType;
import org.example.types.StreetType;
import org.example.utils.Coordinates;

import java.util.List;

public class Player implements Drawable {
    @Getter
    private final int id;
    private final String name;
    private final Token token;
    @Getter
    private Integer money;
    @Getter
    private final int turnNumber;
    @Getter
    private final Deeds deeds;

    public Player(int id, String name, Token token, int turnNumber) {
        this.id = id;
        this.name = name;
        this.token = token;
        this.money = 2000;
        this.turnNumber = turnNumber;
        deeds = new Deeds();
    }

    public String getName() {
        return name.replace("ö", "o").replace("ä", "a").replace("Ö", "O").replace("Ä", "A");
    }

    public void draw(Coordinates coords) {
        token.draw(coords);
    }

    @Override
    public Coordinates getCoords() {
        return token.getCoords();
    }

    @Override
    public void setCoords(Coordinates coords) {
        token.setCoords(coords);
    }

    public Spot getSpot() {
        return token.getSpot();
    }

    public void setSpot(Spot spot) {
        token.getSpot().removePlayer(this);
        spot.addPlayer(this);
        token.setSpot(spot);
    }

    @Override
    public void draw() {
        draw(null);
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

    public List<PropertySpot> getAllDeeds() {
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
        System.out.println(from.getName() + " Didn't have enough money to give");
        return false;
    }

    public List<PropertySpot> getOwnedSpots(StreetType streetType) {
        return deeds.getDeeds(streetType);
    }

    public boolean ownsAllSpots(StreetType streetType) {
        return SpotType.getNumberOfSpots(streetType).equals(getOwnedSpots(streetType).size());
    }
    public Color getColor() {
        return token.getColor();
    }
}
