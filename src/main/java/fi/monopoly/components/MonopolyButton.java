package fi.monopoly.components;

import controlP5.Button;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;

public class MonopolyButton extends Button {
    private ButtonAction buttonAction;

    public MonopolyButton(MonopolyRuntime runtime, String id) {
        super(runtime.controlP5(), id);
        setFont(runtime.font20());
        addListener(e -> {
            if (buttonAction != null) {
                buttonAction.doAction();
            }
        });
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
        return this;
    }

    public void pressButton() {
        if (buttonAction != null) {
            buttonAction.doAction();
        }
    }
}
