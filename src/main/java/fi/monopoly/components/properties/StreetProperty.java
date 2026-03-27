package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@ToString(callSuper = true)
public class StreetProperty extends Property {
    @Getter
    private final int housePrice;
    @Getter
    private int houseCount = 0;
    @Getter
    private int hotelCount = 0;

    public StreetProperty(SpotType spotType) {
        super(spotType);
        housePrice = spotType.getIntegerProperty("housePrice");
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            Integer rentAmount = getRentAmount();
            if (!hasBuildings() && getOwnerPlayer().ownsAllStreetProperties(spotType.streetType)) {
                rentAmount *= 2;
            }
            return rentAmount;
        }
        return 0;
    }

    private Integer getRentAmount() {
        int index = hotelCount == 1 ? 5 : houseCount;
        return getRentPrices().get(index);
    }

    public boolean hasBuildings() {
        return houseCount > 0 || hotelCount > 0;
    }

    public int getBuildingLevel() {
        return hotelCount == 1 ? 5 : houseCount;
    }

    /**
     * Selling flows treat a hotel as five houses, which matches the existing
     * house/hotel transition rules in this class.
     */
    public int getSellableBuildingCount() {
        return houseCount + hotelCount * 5;
    }

    /**
     * Street properties add the sell value of houses/hotels on top of the base
     * property liquidation value.
     */
    @Override
    public int getLiquidationValue() {
        int buildingSellValue = getSellableBuildingCount() * housePrice / 2;
        return buildingSellValue + super.getLiquidationValue();
    }

    public boolean buyHouses(int count) {
        if (fi.monopoly.components.Game.isDebtResolutionForCurrentTurn()) {
            showPopup("streetProperty.debt.noBuy");
            return false;
        }
        if (!canBuyHouses(count)) {
            if (ownerPlayer == null || ownerPlayer.getMoneyAmount() >= count * housePrice) {
                showPopup("streetProperty.buy.mustBuildEvenly");
            } else {
                showPopup("streetProperty.buy.notEnough", count);
            }
            return false;
        }
        for (int i = 0; i < count; i++) {
            ownerPlayer.addMoney(-housePrice);
            houseCount += 1;
            updateHotelCount();
        }
        return true;
    }

    private void updateHotelCount() {
        if (hotelCount == 1 && houseCount < 0) {
            hotelCount = 0;
            houseCount = 5 + houseCount;
        }
        if (houseCount >= 5) {
            //Handle hotel
            houseCount = 0;
            hotelCount = 1;
        }
    }

    public boolean sellHouses(int count) {
        if (!canSellHouses(count)) {
            showPopup("streetProperty.sell.mustSellEvenly");
            return false;
        }
        for (int i = 0; i < count; i++) {
            ownerPlayer.addMoney(housePrice / 2);
            houseCount -= 1;
            updateHotelCount();
        }
        return true;
    }

    public boolean canBuyHouses(int count) {
        if (count <= 0 || ownerPlayer == null || !ownerPlayer.ownsAllStreetProperties(streetType())) {
            return false;
        }
        if (ownerPlayer.getOwnedProperties(streetType()).stream().anyMatch(Property::isMortgaged)) {
            return false;
        }
        int simulatedLevel = getBuildingLevel();
        int availableMoney = ownerPlayer.getMoneyAmount();
        for (int i = 0; i < count; i++) {
            if (simulatedLevel >= 5 || availableMoney < housePrice) {
                return false;
            }
            int simulatedCurrentLevel = simulatedLevel;
            int minGroupLevel = getStreetSetProperties().stream()
                    .mapToInt(property -> property == this ? simulatedCurrentLevel : property.getBuildingLevel())
                    .min()
                    .orElse(simulatedCurrentLevel);
            if (simulatedLevel > minGroupLevel) {
                return false;
            }
            simulatedLevel += 1;
            availableMoney -= housePrice;
        }
        return true;
    }

    public boolean canSellHouses(int count) {
        if (count <= 0 || ownerPlayer == null) {
            return false;
        }
        int simulatedLevel = getBuildingLevel();
        for (int i = 0; i < count; i++) {
            if (simulatedLevel <= 0) {
                return false;
            }
            int simulatedCurrentLevel = simulatedLevel;
            int maxGroupLevel = getStreetSetProperties().stream()
                    .mapToInt(property -> property == this ? simulatedCurrentLevel : property.getBuildingLevel())
                    .max()
                    .orElse(simulatedCurrentLevel);
            if (simulatedLevel < maxGroupLevel) {
                return false;
            }
            simulatedLevel -= 1;
        }
        return true;
    }

    public int getMaxBuyableHouseCount() {
        int count = 0;
        while (canBuyHouses(count + 1)) {
            count++;
        }
        return count;
    }

    public boolean buyBuildingRoundsAcrossSet(int rounds) {
        if (!canBuyBuildingRoundsAcrossSet(rounds)) {
            if (ownerPlayer == null || ownerPlayer.getMoneyAmount() >= getStreetSetRoundCost(rounds)) {
                showPopup("streetProperty.buy.mustBuildEvenly");
            } else {
                showPopup("streetProperty.buy.notEnoughSet", rounds);
            }
            return false;
        }
        List<StreetProperty> properties = getStreetSetProperties();
        for (int round = 0; round < rounds; round++) {
            for (StreetProperty property : properties) {
                property.buyHouses(1);
            }
        }
        return true;
    }

    public boolean canBuyBuildingRoundsAcrossSet(int rounds) {
        List<StreetProperty> properties = getStreetSetProperties();
        if (rounds <= 0 || ownerPlayer == null || !ownerPlayer.ownsAllStreetProperties(streetType())) {
            return false;
        }
        if (ownerPlayer.getOwnedProperties(streetType()).stream().anyMatch(Property::isMortgaged)) {
            return false;
        }
        int[] levels = properties.stream().mapToInt(StreetProperty::getBuildingLevel).toArray();
        int availableMoney = ownerPlayer.getMoneyAmount();
        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < properties.size(); i++) {
                StreetProperty property = properties.get(i);
                if (levels[i] >= 5 || availableMoney < property.getHousePrice()) {
                    return false;
                }
                int minGroupLevel = java.util.Arrays.stream(levels).min().orElse(levels[i]);
                if (levels[i] > minGroupLevel) {
                    return false;
                }
                levels[i] += 1;
                availableMoney -= property.getHousePrice();
            }
        }
        return true;
    }

    public int getMaxBuyableBuildingRoundsAcrossSet() {
        int rounds = 0;
        while (canBuyBuildingRoundsAcrossSet(rounds + 1)) {
            rounds++;
        }
        return rounds;
    }

    public int getMaxSellableHouseCount() {
        int count = 0;
        while (canSellHouses(count + 1)) {
            count++;
        }
        return count;
    }

    public int getStreetSetRoundCost(int rounds) {
        if (rounds <= 0) {
            return 0;
        }
        return getStreetSetProperties().stream()
                .mapToInt(property -> property.getHousePrice() * rounds)
                .sum();
    }

    private List<StreetProperty> getStreetSetProperties() {
        if (ownerPlayer == null) {
            return List.of(this);
        }
        return ownerPlayer.getOwnedProperties(streetType()).stream()
                .map(property -> (StreetProperty) property)
                .sorted(Comparator.comparingInt(property -> property.getSpotType().ordinal()))
                .toList();
    }

    private StreetType streetType() {
        return spotType.streetType;
    }

    private void showPopup(String key, Object... args) {
        if (ownerPlayer != null && ownerPlayer.getRuntime() != null) {
            ownerPlayer.getRuntime().popupService().show(text(key, args));
        }
    }
}
