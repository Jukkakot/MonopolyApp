package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.types.SpotType;
import lombok.Getter;
import lombok.ToString;

@ToString(callSuper = true)
public class StreetProperty extends Property {
    private final int housePrice;
    @Getter
    private int houseCount = 1;
    @Getter
    private int hotelCount = 0;

    public StreetProperty(SpotType spotType) {
        super(spotType);
        housePrice = spotType.getIntegerProperty("housePrice");
    }

    @Override
    public Integer getRent(Player player) {
        if (hasOwner() && isNotOwner(player)) {
            Integer rentAmount = getRentPrices().get(houseCount);
            if (getOwnerPlayer().ownsAllStreetProperties(spotType.streetType)) {
                return rentAmount * 2;
            } else {
                return rentAmount;
            }
        }
        return 0;
    }

    public boolean hasAnyBuildings() {
        return houseCount > 0 || hotelCount > 0;
    }

    public boolean buyHouses(int count) {
        if (ownerPlayer.addMoney(-count * housePrice)) {
            houseCount += count;
        } else {
            Popup.show("You dont have enough money to buy " + count + " houses");
            return false;
        }
        updateHotelCount();
        return true;
    }

    private void updateHotelCount() {
        if (hotelCount == 1 && houseCount < 0) {
            hotelCount = 0;
            houseCount = 5 +houseCount;
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
