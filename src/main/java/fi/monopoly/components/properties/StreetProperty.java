package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.types.SpotType;
import lombok.Getter;
import lombok.ToString;

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

    /**
     * Selling flows treat a hotel as five houses, which matches the existing
     * house/hotel transition rules in this class.
     */
    public int getSellableBuildingCount() {
        return houseCount + hotelCount * 5;
    }

    @Override
    public int getLiquidationValue() {
        int buildingSellValue = getSellableBuildingCount() * housePrice / 2;
        return buildingSellValue + super.getLiquidationValue();
    }

    public boolean buyHouses(int count) {
        if (fi.monopoly.components.Game.isDebtResolutionForCurrentTurn()) {
            if (ownerPlayer != null && ownerPlayer.getRuntime() != null) {
                ownerPlayer.getRuntime().popupService().show(text("streetProperty.debt.noBuy"));
            }
            return false;
        }
        if (ownerPlayer.addMoney(-count * housePrice)) {
            houseCount += count;
        } else {
            if (ownerPlayer != null && ownerPlayer.getRuntime() != null) {
                ownerPlayer.getRuntime().popupService().show(text("streetProperty.buy.notEnough", count));
            }
            return false;
        }
        updateHotelCount();
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
        if (ownerPlayer.addMoney(count * housePrice / 2)) {
            houseCount -= count;
        }
        updateHotelCount();
        return true;
    }
}
