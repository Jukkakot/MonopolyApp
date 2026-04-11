package fi.monopoly.components;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.Properties;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
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

import java.util.Deque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;

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
    private final ComputerPlayerProfile computerProfile;
    @Getter
    private int getOutOfJailCardCount = 0;
    private final Deque<StreetType> getOutOfJailCardSources = new LinkedList<>();

    public Player(MonopolyRuntime runtime, String name, Color color, Spot spot) {
        this(runtime, name, color, spot, ComputerPlayerProfile.HUMAN);
    }

    public Player(MonopolyRuntime runtime, String name, Color color, Spot spot, ComputerPlayerProfile computerProfile) {
        super(runtime, spot, color);
        this.id = NEXT_ID++;
        this.name = name;
        this.moneyAmount = STARTING_MONEY_AMOUNT;
        this.computerProfile = computerProfile;
        // turn number is id by default. Later maybe implement so that this can change
        this.turnNumber = id + 1; // Turn numbers starts from 1
        setSpot(spot);
    }

    public Player(String name, Color color, int moneyAmount, int turnNumber) {
        this(name, color, moneyAmount, turnNumber, ComputerPlayerProfile.HUMAN);
    }

    public Player(String name, Color color, int moneyAmount, int turnNumber, ComputerPlayerProfile computerProfile) {
        super(null, new Coordinates(), color);
        this.id = turnNumber - 1;
        this.name = name;
        this.moneyAmount = moneyAmount;
        this.turnNumber = turnNumber;
        this.computerProfile = computerProfile;
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
        return buyProperty(property, property.getPrice());
    }

    public boolean canBuyProperty(Property property, int price) {
        return property.getOwnerPlayer() == null && price >= 0 && moneyAmount >= price;
    }

    public boolean buyProperty(Property property, int price) {
        if (!canBuyProperty(property, price)) {
            return false;
        }
        addMoney(-price);
        giveProperty(property);
        return true;
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
        List<Property> matchingProperties = new ArrayList<>();
        for (Property property : getOwnedProperties()) {
            if (property.isSameStreetType(streetType)) {
                matchingProperties.add(property);
            }
        }
        return matchingProperties;
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
            transferPropertyTo(targetPlayer, property);
        }
        targetPlayer.setGetOutOfJailCardCount(targetPlayer.getGetOutOfJailCardCount() + getOutOfJailCardCount);
        targetPlayer.getOutOfJailCardSources.addAll(getOutOfJailCardSources);
        getOutOfJailCardSources.clear();
        getOutOfJailCardCount = 0;
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

    public boolean transferPropertyTo(Player targetPlayer, Property property) {
        if (targetPlayer == null || property == null || property.getOwnerPlayer() != this) {
            return false;
        }
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
        return true;
    }

    public boolean transferGetOutOfJailCardTo(Player targetPlayer) {
        if (targetPlayer == null || !hasGetOutOfJailCard()) {
            return false;
        }
        targetPlayer.addOutOfJailCard(getOutOfJailCardSources.pollFirst());
        getOutOfJailCardCount--;
        return true;
    }

    public int liquidateBuildingsToBank() {
        int startingMoney = moneyAmount;
        boolean soldBuilding;
        do {
            soldBuilding = false;
            List<StreetProperty> streetProperties = getOwnedProperties().stream()
                    .filter(StreetProperty.class::isInstance)
                    .map(StreetProperty.class::cast)
                    .sorted(Comparator.comparingInt(StreetProperty::getBuildingLevel).reversed()
                            .thenComparingInt(property -> property.getSpotType().ordinal()))
                    .toList();
            for (StreetProperty property : streetProperties) {
                if (property.getBuildingLevel() <= 0 || !property.canSellHouses(1)) {
                    continue;
                }
                if (property.sellHouses(1)) {
                    soldBuilding = true;
                }
            }
        } while (soldBuilding);
        int liquidationCash = moneyAmount - startingMoney;
        if (liquidationCash > 0) {
            log.info("Liquidated buildings for {} and raised M{}", getName(), liquidationCash);
        }
        return liquidationCash;
    }

    public void addOutOfJailCard() {
        addOutOfJailCard(null);
    }

    public void addOutOfJailCard(StreetType sourceStreetType) {
        getOutOfJailCardSources.addLast(sourceStreetType);
        getOutOfJailCardCount++;
    }

    public boolean hasGetOutOfJailCard() {
        return getOutOfJailCardCount > 0;
    }

    public StreetType useGetOutOfJailCard() {
        if (isInJail()) {
            log.error("Tried to use get out of jail when already in jail");
            return null;
        }
        if (hasGetOutOfJailCard()) {
            getOutOfJailCardCount--;
            return getOutOfJailCardSources.pollFirst();
        }
        return null;
    }

    public void setGetOutOfJailCardCount(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("Get out of jail card count cannot be negative");
        }
        while (getOutOfJailCardCount < count) {
            getOutOfJailCardSources.addLast(null);
            getOutOfJailCardCount++;
        }
        while (getOutOfJailCardCount > count) {
            getOutOfJailCardSources.pollLast();
            getOutOfJailCardCount--;
        }
    }

    public int getTotalHouseCount() {
        return ownedProperties.getTotalHouseCount();
    }

    public int getTotalHotelCount() {
        return ownedProperties.getTotalHotelCount();
    }

    /**
     * Sums each property's liquidation value, including subclass-specific
     * overrides such as street building sell value.
     */
    public int getTotalLiquidationValue() {
        int totalLiquidationValue = 0;
        for (Property property : ownedProperties.getProperties()) {
            totalLiquidationValue += property.getLiquidationValue();
        }
        return totalLiquidationValue;
    }

    public boolean isInJail() {
        return JailSpot.jailTimeLeftMap.get(this) != null;
    }

    public boolean isComputerControlled() {
        return computerProfile.isComputerControlled();
    }
}
