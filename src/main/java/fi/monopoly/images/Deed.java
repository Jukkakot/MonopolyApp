package fi.monopoly.images;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Game;
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
    private String name;

    public Deed(MonopolyRuntime runtime, Property property) {
        super(runtime, property.getSpotType());
        this.property = property;
        this.name = property.getSpotType().getStringProperty("name");
    }

    @Override
    public void onClick() {
        super.onClick();
        if (!property.isOwner(Game.players.getTurn())) {
            //No actions if player not owner of the deed
            return;
        }
        if (property instanceof StreetProperty streetProperty) {
            if (!streetProperty.hasBuildings()) {
                handleMortgaging();
            } else if (Game.isDebtResolutionForCurrentTurn()) {
                runtime.popupService().show(
                        text("deed.sellBeforeMortgage", name, getHouseSellValue(streetProperty)),
                        getSellHouseButtons(streetProperty)
                );
            } else {
                //Assuming that mortgaged property should never have buildings on it.
                runtime.popupService().show(text("deed.cannotMortgageWithBuildings"));
                if (property.isMortgaged()) {
                    log.warn("Mortgaged property with buildings? {}", name);
                }
            }
        } else {
            handleMortgaging();
        }
    }

    private ButtonProps[] getSellHouseButtons(StreetProperty streetProperty) {
        int sellableBuildingCount = streetProperty.getSellableBuildingCount();
        ButtonProps[] buttonProps = new ButtonProps[sellableBuildingCount];
        for (int i = 0; i < sellableBuildingCount; i++) {
            final int finalI = i + 1;
            int totalReturn = finalI * getHouseSellValue(streetProperty);
            buttonProps[i] = new ButtonProps(finalI + " (M" + totalReturn + ")", () -> streetProperty.sellHouses(finalI));
        }
        return buttonProps;
    }

    private int getHouseSellValue(StreetProperty streetProperty) {
        return streetProperty.getHousePrice() / 2;
    }

    private void handleMortgaging() {
        if (property.isMortgaged()) {
            if (Game.isDebtResolutionForCurrentTurn()) {
                runtime.popupService().show(text("deed.cannotUnmortgageDuringDebt"));
                return;
            }
            ButtonAction onAccept = () -> {
                if (!property.handleMortgaging()) {
                    runtime.popupService().show(text("deed.notEnoughToUnmortgage", getUnmortgageCost()));
                }
            };
            runtime.popupService().show(text("deed.confirmUnmortgage", name, getUnmortgageCost()), onAccept, null);
        } else {
            runtime.popupService().show(text("deed.confirmMortgage", name, property.getMortgageValue()), property::handleMortgaging, null);
        }
    }

    private int getUnmortgageCost() {
        return property.getMortgageValue() + property.getMortgageInterest();
    }

}
