package fi.monopoly.components.spots;

import fi.monopoly.components.Game;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.popup.components.ButtonProps;
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
        if (!property.isOwner(Game.players.getTurn())) {
            return;
        }
        if (!Game.players.getTurn().ownsAllStreetProperties(spotType.streetType)) {
            Popup.show("You can't buy houses unless you own all same street properties");
            return;
        }
        if (property.hasAnyBuildings()) {
           if(property.getHotelCount() == 1) {
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
        Popup.show("How many houses do you want to buy?", getBuyHousesButtons());
    }

    private void handleSellBuildings() {
        Popup.show("How many houses do you want to sell?", getSellHousesButtons());
    }

    private ButtonProps[] getBuyHousesButtons() {
        int maxHouseCountToBuy = 5 - property.getHouseCount();
        ButtonProps[] buttonProps = new ButtonProps[maxHouseCountToBuy];
        for (int i = 0; i < maxHouseCountToBuy; i++) {
            final int finalI = i + 1;
            buttonProps[i] = new ButtonProps(String.valueOf(finalI), () -> property.buyHouses(finalI));
        }
        return buttonProps;
    }

    private ButtonProps[] getSellHousesButtons() {
        int intMaxHousesToSell = property.getHouseCount() + property.getHotelCount() *5;
        ButtonProps[] buttonProps = new ButtonProps[intMaxHousesToSell];
        for (int i = 0; i < intMaxHousesToSell; i++) {
            final int finalI = i + 1;
            buttonProps[i] = new ButtonProps(String.valueOf(finalI), () -> property.sellHouses(finalI));
        }
        return buttonProps;
    }


}
