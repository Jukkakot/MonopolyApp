package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.properties.Properties;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@ToString
@Slf4j
public class Player extends PlayerToken {
    private static int NEXT_ID = 0;
    @Getter
    private final int id;
    private final String name;
    private final int STARTING_MONEY_AMOUNT = 1500;
    private final Properties ownedProperties = new Properties();
    @Getter
    private Integer moneyAmounnt;
    @Getter
    private int turnNumber;
    @Getter
    @Setter
    private int getOutOfJailCardCount = 0;

    public Player(MonopolyRuntime runtime, String name, Color color, Spot spot) {
        super(runtime, spot, color);
        this.id = NEXT_ID++;
        this.name = name;
        this.moneyAmounnt = STARTING_MONEY_AMOUNT;
        // turn number is id by default. Later maybe implement so that this can change
        this.turnNumber = id + 1; // Turn numbers starts from 1
        setSpot(spot);
    }

    public Player(String name, Color color, int moneyAmount, int turnNumber) {
        super(null, new Coordinates(), color);
        this.id = turnNumber - 1;
        this.name = name;
        this.moneyAmounnt = moneyAmount;
        this.turnNumber = turnNumber;
    }

    public String getName() {
        return MonopolyUtils.parseIllegalCharacters(name);
    }

    public void setSpot(Spot newSpot) {
        spot.removePlayer(this);
        spot = newSpot;
        spot.addPlayer(this);
    }

    private void giveProperty(Property property) {
        property.setOwnerPlayer(this);
        if (!ownedProperties.addProperty(property)) {
            log.error("Player already owned property {}", property);
        }
    }

    public boolean canBuyProperty(Property ps) {
        return ps.getOwnerPlayer() == null && moneyAmounnt >= ps.getPrice();
    }

    public boolean buyProperty(Property property) {
        if (canBuyProperty(property)) {
            addMoney(-property.getPrice());
            giveProperty(property);
            return true;
        }
        return false;
    }

    public boolean addMoney(Integer amount) {
        if (moneyAmounnt + amount >= 0) {
            moneyAmounnt += amount;
            log.trace("Added {} money to {}", amount, name);
            return true;
        }
        return false;
    }

    public boolean giveMoney(Player fromPlayer, Integer amount) {
        if (amount < 0) {
            throw new RuntimeException("Not possible to give negative amount of money.");
        }
        if (fromPlayer.addMoney(-amount)) {
            addMoney(amount);
            return true;
        }
        if (runtime != null) {
            runtime.popupService().show(fromPlayer.getName() + " Didn't have enough money to give to " + name);
        }
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
        if (isInJail()) {
            log.error("Tried to use get out of jail when already in jail");
            return false;
        }
        if (hasGetOutOfJailCard()) {
            getOutOfJailCardCount--;
            return true;
        }
        return false;
    }

    public int getTotalHouseCount() {
        return ownedProperties.getTotalHouseCount();
    }

    public int getTotalHotelCount() {
        return ownedProperties.getTotalHotelCount();
    }

    public boolean isInJail() {
        return JailSpot.jailTimeLeftMap.get(this) != null;
    }
}
