package fi.monopoly.components.spots;

import fi.monopoly.components.GameSession;
import fi.monopoly.components.Player;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.images.SpotImage;
import fi.monopoly.types.SpotType;

import static fi.monopoly.text.UiTexts.text;

public class StreetPropertySpot extends PropertySpot {
    private final StreetProperty property;

    public StreetPropertySpot(SpotImage spotImage, SpotType spotType) {
        super(spotImage, spotType);
        property = (StreetProperty) PropertyFactory.getProperty(spotType);
    }

    @Override
    public void onClick() {
        super.onClick();
        GameSession session = runtime.gameSessionOrNull();
        if (session == null || session.players() == null) {
            return;
        }
        Player turnPlayer = session.players().getTurn();
        if (!property.isOwner(turnPlayer)) {
            //Player not owner of this property
            return;
        }
        if (property.isMortgaged()) {
            //Cannot sell/buy buildings on mortgaged property
            return;
        }
        if (turnPlayer.getOwnedProperties(spotType.streetType).stream().anyMatch(Property::isMortgaged)) {
            runtime.popupService().show(text("streetProperty.mortgaged.cannotBuy"));
            return;
        }
        if (!turnPlayer.ownsAllStreetProperties(spotType.streetType)) {
            runtime.popupService().show(text("streetProperty.requireFullSet"));
            return;
        }
        if (session.isDebtResolutionActive()) {
            if (property.hasBuildings()) {
                handleSellBuildings();
            } else {
                runtime.popupService().show(text("streetProperty.debt.onlySellOrMortgage"));
            }
            return;
        }
        if (property.hasBuildings()) {
            if (property.getHotelCount() == 1) {
                handleSellBuildings();
            } else {
                runtime.popupService().show(text("streetProperty.buyOrSellPrompt", property.getHousePrice(), getHouseSellValue()),
                        new ButtonProps(text("popup.action.buy"), this::handleBuyBuildings),
                        new ButtonProps(text("popup.action.sell"), this::handleSellBuildings));
            }
        } else {
            handleBuyBuildings();
        }

    }

    private void handleBuyBuildings() {
        int maxHouseCount = property.getMaxBuyableHouseCount();
        int maxSetRounds = property.getMaxBuyableBuildingRoundsAcrossSet();
        if (maxHouseCount > 0 || maxSetRounds > 0) {
            runtime.popupService().show(text("streetProperty.buy.countPrompt", property.getHousePrice()), getBuyHousesButtons(maxHouseCount));
        } else {
            runtime.popupService().show(text("streetProperty.buy.cannotAfford"));
        }
    }

    private void handleSellBuildings() {
        runtime.popupService().show(text("streetProperty.sell.countPrompt", getHouseSellValue()), getSellHousesButtons());
    }

    private ButtonProps[] getBuyHousesButtons(int maxHouseCount) {
        int maxSetRounds = property.getMaxBuyableBuildingRoundsAcrossSet();
        ButtonProps[] buttonProps = new ButtonProps[maxHouseCount + maxSetRounds];
        for (int i = 0; i < maxHouseCount; i++) {
            final int finalI = i + 1;
            int totalCost = finalI * property.getHousePrice();
            buttonProps[i] = new ButtonProps(text("streetProperty.buy.singleOption", finalI, totalCost), () -> property.buyHouses(finalI));
        }
        for (int i = 0; i < maxSetRounds; i++) {
            final int finalI = i + 1;
            int totalCost = property.getStreetSetRoundCost(finalI);
            buttonProps[maxHouseCount + i] = new ButtonProps(
                    text("streetProperty.buy.setOption", finalI, totalCost),
                    () -> property.buyBuildingRoundsAcrossSet(finalI));
        }
        return buttonProps;
    }

    private ButtonProps[] getSellHousesButtons() {
        int intMaxHousesToSell = property.getMaxSellableHouseCount();
        int maxSetRounds = property.getMaxSellableBuildingRoundsAcrossSet();
        ButtonProps[] buttonProps = new ButtonProps[intMaxHousesToSell + maxSetRounds];
        for (int i = 0; i < intMaxHousesToSell; i++) {
            final int finalI = i + 1;
            int totalReturn = finalI * getHouseSellValue();
            buttonProps[i] = new ButtonProps(text("format.countMoneyOption", finalI, totalReturn), () -> {
                GameSession session = runtime.gameSessionOrNull();
                if (session != null && session.isDebtResolutionActive() && session.debtActionDispatcher() != null) {
                    session.debtActionDispatcher().sellBuilding(spotType, finalI);
                    return;
                }
                property.sellHouses(finalI);
            });
        }
        for (int i = 0; i < maxSetRounds; i++) {
            final int finalI = i + 1;
            int totalReturn = property.getStreetSetRoundCost(finalI) / 2;
            buttonProps[intMaxHousesToSell + i] = new ButtonProps(
                    text("streetProperty.sell.setOption", finalI, totalReturn),
                    () -> {
                        GameSession session = runtime.gameSessionOrNull();
                        if (session != null && session.isDebtResolutionActive() && session.debtActionDispatcher() != null) {
                            session.debtActionDispatcher().sellBuildingRoundsAcrossSet(spotType, finalI);
                            return;
                        }
                        property.sellBuildingRoundsAcrossSet(finalI);
                    });
        }
        return buttonProps;
    }

    private int getHouseSellValue() {
        return property.getHousePrice() / 2;
    }


}
