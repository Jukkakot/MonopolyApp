package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.MonopolyUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@ToString(exclude = "ownerPlayer")
public abstract class Property {

    @Getter
    protected int price;
    @Getter
    @Setter
    protected boolean isMortgaged = false;
    @Getter
    @Setter
    protected Player ownerPlayer;
    @Getter
    protected List<Integer> rentPrices;
    @Getter
    protected SpotType spotType;
    public static final int MOVE_NEAREST_CARD_MULTIPLIER = 2;

    public Property(SpotType spotType) {
        this.spotType = spotType;
        price = spotType.getIntegerProperty("price");
        String rentStr = spotType.getStringProperty("rents");
        if (rentStr.contains(",")) {
            rentPrices = Arrays.stream(rentStr.split(",")).map(Integer::valueOf).toList();
        }
    }

    public boolean hasOwner() {
        return ownerPlayer != null;
    }

    public boolean isOwner(Player p) {
        return hasOwner() && ownerPlayer.equals(p);
    }

    public boolean isNotOwner(Player p) {
        return !isOwner(p);
    }

    public boolean isSameStreetType(StreetType streetType) {
        return spotType.streetType.equals(streetType);
    }

    public String getDisplayName() {
        String spotName = spotType.getStringProperty("name");
        return spotName.isBlank() ? spotType.name() : MonopolyUtils.parseIllegalCharacters(spotName);
    }

    public abstract Integer getRent(Player player);

    public int getMortgageValue() {
        return getPrice() / 2;
    }

    public int getMortgageInterest() {
        return (int) (getMortgageValue() * 0.1);
    }

    /**
     * Returns the amount of cash this property can contribute in an immediate
     * liquidation scenario. Subclasses may extend this with additional
     * asset-specific value such as sellable buildings.
     */
    public int getLiquidationValue() {
        return isMortgaged() ? 0 : getMortgageValue();
    }

    public boolean handleMortgaging() {
        if (!hasOwner()) {
            log.error("Property does not have a owner.");
            return false;
        }

        int mortgageAmount = getMortgageValue();
        if (isMortgaged()) {
            int interest = getMortgageInterest();
            int unMortageAmount = mortgageAmount + interest;
            if (ownerPlayer.addMoney(-unMortageAmount)) {
                setMortgaged(false);
            } else {
                //Not enough money to unmortgage
                return false;
            }
        } else {
            setMortgaged(true);
            ownerPlayer.addMoney(mortgageAmount);
        }
        return true;
    }

    public void restoreState(Player ownerPlayer, boolean mortgaged) {
        this.ownerPlayer = ownerPlayer;
        this.isMortgaged = mortgaged;
    }

    void resetState() {
        ownerPlayer = null;
        isMortgaged = false;
    }
}
