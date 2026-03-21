package fi.monopoly.components.spots;

import fi.monopoly.components.Game;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.SpotType;

public class StreetPropertySpot extends PropertySpot {
    private final StreetProperty property;

    public StreetPropertySpot(SpotImage spotImage, SpotType spotType) {
        super(spotImage, spotType);
        property = (StreetProperty) PropertyFactory.getProperty(spotType);
    }

    @Override
    public void onClick() {
        super.onClick();
        Player turnPlayer = Game.players.getTurn();
        if (!property.isOwner(turnPlayer)) {
            //Player not owner of this property
            return;
        }
        if (property.isMortgaged()) {
            //Cannot sell/buy buildings on mortgaged property
            return;
        }
        if(turnPlayer.getOwnedProperties(spotType.streetType).stream().anyMatch(Property::isMortgaged)) {
            Popup.show("Cannot buy buildings when some of the properties are mortgaged");
            return;
        }
        if (!turnPlayer.ownsAllStreetProperties(spotType.streetType)) {
            Popup.show("You can't buy houses unless you own all same street properties");
            return;
        }
        if (property.hasBuildings()) {
            if (property.getHotelCount() == 1) {
                handleSellBuildings();
            } else {
                Popup.show("Do you want to buy or sell houses?",
                        new ButtonProps("Buy", this::handleBuyBuildings),
                        new ButtonProps("Sell", this::handleSellBuildings));
            }
        } else {
            handleBuyBuildings();
        }

    }

    private void handleBuyBuildings() {
        int maxHouseCount = getMaxHouseCountToBuy();
        if (maxHouseCount > 0) {
            Popup.show("How many houses do you want to buy? Houses cost M" + property.getHousePrice() + " each.", getBuyHousesButtons(maxHouseCount));
        } else {
            Popup.show("You cannot afford buying houses");
        }
    }

    private void handleSellBuildings() {
        Popup.show("How many houses do you want to sell?", getSellHousesButtons());
    }

    private ButtonProps[] getBuyHousesButtons(int maxHouseCount) {
        ButtonProps[] buttonProps = new ButtonProps[maxHouseCount];
        for (int i = 0; i < maxHouseCount; i++) {
            final int finalI = i + 1;
            buttonProps[i] = new ButtonProps(String.valueOf(finalI), () -> property.buyHouses(finalI));
        }
        return buttonProps;
    }

    private int getMaxHouseCountToBuy() {
        //How many houses can property fit still
        int maxHouseCountToBuy = 5 - property.getHouseCount();
        //How many houses can player afford
        int maxHouseCountAfford = Game.players.getTurn().getMoneyAmounnt() / property.getHousePrice();

        int maxHouseCount = Math.min(maxHouseCountToBuy, maxHouseCountAfford);
        return maxHouseCount;
    }

    private ButtonProps[] getSellHousesButtons() {
        int intMaxHousesToSell = property.getHouseCount() + property.getHotelCount() * 5;
        ButtonProps[] buttonProps = new ButtonProps[intMaxHousesToSell];
        for (int i = 0; i < intMaxHousesToSell; i++) {
            final int finalI = i + 1;
            buttonProps[i] = new ButtonProps(String.valueOf(finalI), () -> property.sellHouses(finalI));
        }
        return buttonProps;
    }


}
