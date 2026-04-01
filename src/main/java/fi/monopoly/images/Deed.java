package fi.monopoly.images;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.GameSession;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
@ToString(callSuper = true)
public class Deed extends AbstractClickable {
    @Getter
    private final Property property;

    public Deed(MonopolyRuntime runtime, Property property) {
        super(runtime, property.getSpotType());
        this.property = property;
    }

    @Override
    public void onClick() {
        super.onClick();
        GameSession session = runtime.gameSessionOrNull();
        if (session == null || session.players() == null || !property.isOwner(session.players().getTurn())) {
            //No actions if player not owner of the deed
            return;
        }
        if (property instanceof StreetProperty streetProperty) {
            if (!streetProperty.hasBuildings()) {
                handleMortgaging();
            } else if (session.isDebtResolutionActive()) {
                runtime.popupService().show(
                        text("deed.sellBeforeMortgage", property.getDisplayName(), getHouseSellValue(streetProperty)),
                        getSellHouseButtons(streetProperty)
                );
            } else {
                //Assuming that mortgaged property should never have buildings on it.
                runtime.popupService().show(text("deed.cannotMortgageWithBuildings"));
                if (property.isMortgaged()) {
                    log.warn("Mortgaged property with buildings? {}", property.getDisplayName());
                }
            }
        } else {
            handleMortgaging();
        }
    }

    private ButtonProps[] getSellHouseButtons(StreetProperty streetProperty) {
        int sellableBuildingCount = streetProperty.getSellableBuildingCount();
        int maxSetRounds = streetProperty.getMaxSellableBuildingRoundsAcrossSet();
        ButtonProps[] buttonProps = new ButtonProps[sellableBuildingCount + maxSetRounds];
        for (int i = 0; i < sellableBuildingCount; i++) {
            final int finalI = i + 1;
            int totalReturn = finalI * getHouseSellValue(streetProperty);
            buttonProps[i] = new ButtonProps(text("format.countMoneyOption", finalI, totalReturn), () -> streetProperty.sellHouses(finalI));
        }
        for (int i = 0; i < maxSetRounds; i++) {
            final int finalI = i + 1;
            int totalReturn = streetProperty.getStreetSetRoundCost(finalI) / 2;
            buttonProps[sellableBuildingCount + i] = new ButtonProps(
                    text("streetProperty.sell.setOption", finalI, totalReturn),
                    () -> streetProperty.sellBuildingRoundsAcrossSet(finalI));
        }
        return buttonProps;
    }

    private int getHouseSellValue(StreetProperty streetProperty) {
        return streetProperty.getHousePrice() / 2;
    }

    private void handleMortgaging() {
        GameSession session = runtime.gameSessionOrNull();
        if (property.isMortgaged()) {
            if (session != null && session.isDebtResolutionActive()) {
                runtime.popupService().show(text("deed.cannotUnmortgageDuringDebt"));
                return;
            }
            ButtonAction onAccept = () -> {
                if (!property.handleMortgaging()) {
                    runtime.popupService().show(text("deed.notEnoughToUnmortgage", getUnmortgageCost()));
                }
            };
            runtime.popupService().show(text("deed.confirmUnmortgage", property.getDisplayName(), getUnmortgageCost()), onAccept, null);
        } else {
            runtime.popupService().show(text("deed.confirmMortgage", property.getDisplayName(), property.getMortgageValue()), property::handleMortgaging, null);
        }
    }

    private int getUnmortgageCost() {
        return property.getMortgageValue() + property.getMortgageInterest();
    }

}
