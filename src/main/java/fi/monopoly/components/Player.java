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

import static fi.monopoly.text.UiTexts.text;

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
    private Integer moneyAmount;
    @Getter
    private int turnNumber;
    @Getter
    @Setter
    private int getOutOfJailCardCount = 0;

    public Player(MonopolyRuntime runtime, String name, Color color, Spot spot) {
        super(runtime, spot, color);
        this.id = NEXT_ID++;
        this.name = name;
        this.moneyAmount = STARTING_MONEY_AMOUNT;
        // turn number is id by default. Later maybe implement so that this can change
        this.turnNumber = id + 1; // Turn numbers starts from 1
        setSpot(spot);
    }

    public Player(String name, Color color, int moneyAmount, int turnNumber) {
        super(null, new Coordinates(), color);
        this.id = turnNumber - 1;
        this.name = name;
        this.moneyAmount = moneyAmount;
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
        return ps.getOwnerPlayer() == null && moneyAmount >= ps.getPrice();
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
        if (moneyAmount + amount >= 0) {
            moneyAmount += amount;
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
            runtime.popupService().show(text("player.giveMoney.notEnough", fromPlayer.getName(), name));
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

    public boolean addOwnedProperty(Property property) {
        Player previousOwner = property.getOwnerPlayer();
        if (previousOwner != null && previousOwner != this) {
            previousOwner.ownedProperties.removeProperty(property);
        }
        property.setOwnerPlayer(this);
        return ownedProperties.addProperty(property);
    }

    public boolean removeOwnedProperty(Property property) {
        boolean removed = ownedProperties.removeProperty(property);
        if (removed && property.getOwnerPlayer() == this) {
            property.setOwnerPlayer(null);
        }
        return removed;
    }

    public void transferAssetsTo(Player targetPlayer) {
        log.info("Transferring assets from {} to {}", getName(), targetPlayer.getName());
        for (Property property : List.copyOf(getOwnedProperties())) {
            ownedProperties.removeProperty(property);
            property.setOwnerPlayer(targetPlayer);
            targetPlayer.ownedProperties.addProperty(property);
            log.debug("Transferred property {} to {}. mortgaged={}",
                    property.getDisplayName(), targetPlayer.getName(), property.isMortgaged());
            if (property.isMortgaged()) {
                int interest = property.getMortgageInterest();
                if (targetPlayer.addMoney(-interest)) {
                    log.info("Mortgage transfer interest M{} paid by {}", interest, targetPlayer.getName());
                } else {
                    log.warn("{} could not pay immediate mortgage transfer interest M{} for {}",
                            targetPlayer.getName(), interest, property.getDisplayName());
                }
            }
        }
        targetPlayer.setGetOutOfJailCardCount(targetPlayer.getGetOutOfJailCardCount() + getOutOfJailCardCount);
        if (moneyAmount > 0) {
            targetPlayer.addMoney(moneyAmount);
            moneyAmount = 0;
        }
    }

    public void releaseAssetsToBank() {
        log.info("Releasing assets from {} to bank", getName());
        for (Property property : List.copyOf(getOwnedProperties())) {
            ownedProperties.removeProperty(property);
            property.setOwnerPlayer(null);
            property.setMortgaged(false);
            log.debug("Released property {} to bank", property.getDisplayName());
        }
        moneyAmount = 0;
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

    public int getTotalLiquidationValue() {
        return ownedProperties.getProperties().stream()
                .mapToInt(Property::getLiquidationValue)
                .sum();
    }

    public boolean isInJail() {
        return JailSpot.jailTimeLeftMap.get(this) != null;
    }
}
