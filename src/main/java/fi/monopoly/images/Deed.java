package fi.monopoly.images;

import fi.monopoly.components.Game;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.components.popup.Popup;
import fi.monopoly.components.properties.Property;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString(callSuper = true)
public class Deed extends Clickable {
    @Getter
    private final Property property;
    private String name;

    public Deed(Property property) {
        super(property.getSpotType());
        this.property = property;
        this.name = property.getSpotType().getStringProperty("name");
    }

    @Override
    public void onClick() {
        super.onClick();
        if (property.isOwner(Game.players.getTurn())) {
            handleMortgaging();
        }
    }

    private void handleMortgaging() {
        if (property.isMortgaged()) {
            ButtonAction onAccept = () -> {
                if (!property.handleMortgaging()) {
                    Popup.show("Player does not have enough money to unmortgage this property");
                }
            };
            Popup.show("Do you want to mortgage " + name + " property?", onAccept, null);
        } else {
            Popup.show("Do you want to unmortgage " + name + " property?", property::handleMortgaging, null);
        }
    }

}
