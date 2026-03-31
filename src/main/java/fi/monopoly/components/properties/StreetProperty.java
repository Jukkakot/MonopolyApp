package fi.monopoly.components.properties;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import lombok.Getter;
import lombok.ToString;

import java.util.Comparator;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@ToString(callSuper = true)
public class StreetProperty extends Property {
    public static final int BANK_HOUSE_SUPPLY = 32;
    public static final int BANK_HOTEL_SUPPLY = 12;
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
        if (isDebtResolutionActive()) {
            showPopup("streetProperty.debt.noBuy");
            return false;
        }
        BuildValidationResult result = validateBuyHouses(count);
        if (result != BuildValidationResult.OK) {
            showValidationPopup(result, count);
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
            houseCount = 0;
            hotelCount = 1;
        }
    }

    public boolean sellHouses(int count) {
        BuildValidationResult result = validateSellHouses(count);
        if (result != BuildValidationResult.OK) {
            showValidationPopup(result, count);
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
        return validateBuyHouses(count) == BuildValidationResult.OK;
    }

    private BuildValidationResult validateBuyHouses(int count) {
        if (count <= 0 || ownerPlayer == null) {
            return BuildValidationResult.INVALID_COUNT;
        }
        if (!ownerPlayer.ownsAllStreetProperties(streetType())) {
            return BuildValidationResult.REQUIRES_FULL_SET;
        }
        if (ownerPlayer.getOwnedProperties(streetType()).stream().anyMatch(Property::isMortgaged)) {
            return BuildValidationResult.MORTGAGED_PROPERTY;
        }
        int simulatedLevel = getBuildingLevel();
        int availableMoney = ownerPlayer.getMoneyAmount();
        int builtHouses = getBuiltHouseCount();
        int builtHotels = getBuiltHotelCount();
        for (int i = 0; i < count; i++) {
            if (simulatedLevel >= 5) {
                return BuildValidationResult.MUST_BUILD_EVENLY;
            }
            if (availableMoney < housePrice) {
                return BuildValidationResult.NOT_ENOUGH_MONEY;
            }
            int simulatedCurrentLevel = simulatedLevel;
            int minGroupLevel = getStreetSetProperties().stream()
                    .mapToInt(property -> property == this ? simulatedCurrentLevel : property.getBuildingLevel())
                    .min()
                    .orElse(simulatedCurrentLevel);
            if (simulatedLevel > minGroupLevel) {
                return BuildValidationResult.MUST_BUILD_EVENLY;
            }
            if (simulatedLevel == 4) {
                if (builtHotels >= BANK_HOTEL_SUPPLY) {
                    return BuildValidationResult.NO_HOTELS_LEFT;
                }
                builtHotels += 1;
                builtHouses -= 4;
            } else {
                if (builtHouses >= BANK_HOUSE_SUPPLY) {
                    return BuildValidationResult.NO_HOUSES_LEFT;
                }
                builtHouses += 1;
            }
            simulatedLevel += 1;
            availableMoney -= housePrice;
        }
        return BuildValidationResult.OK;
    }

    public boolean canSellHouses(int count) {
        return validateSellHouses(count) == BuildValidationResult.OK;
    }

    private BuildValidationResult validateSellHouses(int count) {
        if (count <= 0 || ownerPlayer == null) {
            return BuildValidationResult.INVALID_COUNT;
        }
        int simulatedLevel = getBuildingLevel();
        int builtHouses = getBuiltHouseCount();
        int builtHotels = getBuiltHotelCount();
        for (int i = 0; i < count; i++) {
            if (simulatedLevel <= 0) {
                return BuildValidationResult.MUST_SELL_EVENLY;
            }
            int simulatedCurrentLevel = simulatedLevel;
            int maxGroupLevel = getStreetSetProperties().stream()
                    .mapToInt(property -> property == this ? simulatedCurrentLevel : property.getBuildingLevel())
                    .max()
                    .orElse(simulatedCurrentLevel);
            if (simulatedLevel < maxGroupLevel) {
                return BuildValidationResult.MUST_SELL_EVENLY;
            }
            if (simulatedLevel == 5) {
                if (builtHouses + 4 > BANK_HOUSE_SUPPLY) {
                    return BuildValidationResult.NO_HOUSES_TO_BREAK_HOTEL;
                }
                builtHotels -= 1;
                builtHouses += 4;
            } else {
                builtHouses -= 1;
            }
            simulatedLevel -= 1;
        }
        return BuildValidationResult.OK;
    }

    public int getMaxBuyableHouseCount() {
        int count = 0;
        while (canBuyHouses(count + 1)) {
            count++;
        }
        return count;
    }

    public boolean buyBuildingRoundsAcrossSet(int rounds) {
        BuildValidationResult result = validateBuyBuildingRoundsAcrossSet(rounds);
        if (result != BuildValidationResult.OK) {
            showValidationPopup(result, rounds);
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
        return validateBuyBuildingRoundsAcrossSet(rounds) == BuildValidationResult.OK;
    }

    private BuildValidationResult validateBuyBuildingRoundsAcrossSet(int rounds) {
        List<StreetProperty> properties = getStreetSetProperties();
        if (rounds <= 0 || ownerPlayer == null) {
            return BuildValidationResult.INVALID_COUNT;
        }
        if (!ownerPlayer.ownsAllStreetProperties(streetType())) {
            return BuildValidationResult.REQUIRES_FULL_SET;
        }
        if (ownerPlayer.getOwnedProperties(streetType()).stream().anyMatch(Property::isMortgaged)) {
            return BuildValidationResult.MORTGAGED_PROPERTY;
        }
        int[] levels = properties.stream().mapToInt(StreetProperty::getBuildingLevel).toArray();
        int availableMoney = ownerPlayer.getMoneyAmount();
        int builtHouses = getBuiltHouseCount();
        int builtHotels = getBuiltHotelCount();
        for (int round = 0; round < rounds; round++) {
            for (int i = 0; i < properties.size(); i++) {
                StreetProperty property = properties.get(i);
                if (levels[i] >= 5) {
                    return BuildValidationResult.MUST_BUILD_EVENLY;
                }
                if (availableMoney < property.getHousePrice()) {
                    return BuildValidationResult.NOT_ENOUGH_MONEY;
                }
                int minGroupLevel = java.util.Arrays.stream(levels).min().orElse(levels[i]);
                if (levels[i] > minGroupLevel) {
                    return BuildValidationResult.MUST_BUILD_EVENLY;
                }
                if (levels[i] == 4) {
                    if (builtHotels >= BANK_HOTEL_SUPPLY) {
                        return BuildValidationResult.NO_HOTELS_LEFT;
                    }
                    builtHotels += 1;
                    builtHouses -= 4;
                } else {
                    if (builtHouses >= BANK_HOUSE_SUPPLY) {
                        return BuildValidationResult.NO_HOUSES_LEFT;
                    }
                    builtHouses += 1;
                }
                levels[i] += 1;
                availableMoney -= property.getHousePrice();
            }
        }
        return BuildValidationResult.OK;
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

    private void showValidationPopup(BuildValidationResult result, int count) {
        switch (result) {
            case NOT_ENOUGH_MONEY -> {
                if (count > 1 && getStreetSetProperties().size() > 1) {
                    showPopup("streetProperty.buy.notEnoughSet", count);
                } else {
                    showPopup("streetProperty.buy.notEnough", count);
                }
            }
            case NO_HOUSES_LEFT -> showPopup("streetProperty.bank.noHouses");
            case NO_HOTELS_LEFT -> showPopup("streetProperty.bank.noHotels");
            case NO_HOUSES_TO_BREAK_HOTEL -> showPopup("streetProperty.sell.bank.noHouses");
            case MORTGAGED_PROPERTY -> showPopup("streetProperty.mortgaged.cannotBuy");
            case REQUIRES_FULL_SET -> showPopup("streetProperty.requireFullSet");
            case MUST_SELL_EVENLY -> showPopup("streetProperty.sell.mustSellEvenly");
            default -> showPopup("streetProperty.buy.mustBuildEvenly");
        }
    }

    private int getBuiltHouseCount() {
        Players players = getPlayersOrNull();
        if (players == null) {
            return 0;
        }
        return players.getTotalHouseCount();
    }

    private int getBuiltHotelCount() {
        Players players = getPlayersOrNull();
        if (players == null) {
            return 0;
        }
        return players.getTotalHotelCount();
    }

    private boolean isDebtResolutionActive() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        return runtime != null && runtime.gameSessionOrNull() != null && runtime.gameSessionOrNull().isDebtResolutionActive();
    }

    private Players getPlayersOrNull() {
        MonopolyRuntime runtime = MonopolyRuntime.peek();
        if (runtime == null || runtime.gameSessionOrNull() == null) {
            return null;
        }
        return runtime.gameSessionOrNull().players();
    }

    private enum BuildValidationResult {
        OK,
        INVALID_COUNT,
        REQUIRES_FULL_SET,
        MORTGAGED_PROPERTY,
        NOT_ENOUGH_MONEY,
        MUST_BUILD_EVENLY,
        MUST_SELL_EVENLY,
        NO_HOUSES_LEFT,
        NO_HOTELS_LEFT,
        NO_HOUSES_TO_BREAK_HOTEL
    }

    @Override
    void resetState() {
        super.resetState();
        houseCount = 0;
        hotelCount = 0;
    }
}
