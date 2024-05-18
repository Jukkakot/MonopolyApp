package org.example.images;

import lombok.Getter;
import lombok.ToString;
import org.example.components.Game;
import org.example.components.popup.ButtonAction;
import org.example.components.popup.Popup;
import org.example.components.properties.Property;

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
        System.out.println("Clicked deed " + name);
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
