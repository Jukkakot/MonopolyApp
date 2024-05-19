package fi.monopoly.components;

import controlP5.Button;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.popup.ButtonAction;

public class MonopolyButton extends Button {
    private ButtonAction buttonAction;

    public MonopolyButton(String id) {
        super(MonopolyApp.p5, id);
        setFont(MonopolyApp.font20);
    }

    public MonopolyButton setPosition(Coordinates coordinates) {
        setPosition(coordinates.x(), coordinates.y());
        return this;
    }

    public MonopolyButton setLabel(String label) {
        this.getCaptionLabel().setText(MonopolyUtils.parseIllegalCharacters(label));
        return this;
    }

    public MonopolyButton addListener(ButtonAction buttonAction) {
        this.buttonAction = buttonAction;
        addListener(e -> buttonAction.doAction());
        return this;
    }

    public void pressButton() {
        if (buttonAction != null) {
            buttonAction.doAction();
        }
    }
}
