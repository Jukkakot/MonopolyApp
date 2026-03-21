package fi.monopoly.images;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Game;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

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
            } else {
                //Assuming that mortgaged property should never have buildings on it.
                runtime.popupService().show("Cannot mortgage a property with buildings on it.");
                if (property.isMortgaged()) {
                    log.warn("Mortgaged property with buildings? " + name);
                }
            }
        } else {
            handleMortgaging();
        }
    }

    private void handleMortgaging() {
        if (property.isMortgaged()) {
            ButtonAction onAccept = () -> {
                if (!property.handleMortgaging()) {
                    runtime.popupService().show("Player does not have enough money to unmortgage this property");
                }
            };
            runtime.popupService().show("Do you want to mortgage " + name + " property?", onAccept, null);
        } else {
            runtime.popupService().show("Do you want to unmortgage " + name + " property?", property::handleMortgaging, null);
        }
    }

}
