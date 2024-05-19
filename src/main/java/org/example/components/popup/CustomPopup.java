package org.example.components.popup;

import controlP5.Button;
import controlP5.Controller;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.components.MonopolyButton;
import org.example.components.popup.components.ButtonProps;
import org.example.utils.Coordinates;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomPopup extends Popup {
    private List<Button> customButtons = new ArrayList<>();

    private final Button closeButton = new MonopolyButton("close")
            .setPosition(coords.x() + (float) width / 2 - (float) width / 10, coords.y() - (float) height / 2 + (float) height / 10)
            .addListener(e -> allButtonAction())
            .setLabel("X")
            .hide()
            .setSize(30, 30);

    public void setButtons(ButtonProps... buttonProps) {
        for (ButtonProps buttonProp : buttonProps) {
            customButtons.add(getButton(buttonProp));
        }
    }

    private Button getButton(ButtonProps buttonProps) {
        return new MonopolyButton("customButton" + customButtons.size())
                .setPosition(getButtonCoords(buttonProps))
                .setLabel(buttonProps.name())
                .addListener(e -> getButtonAction(e, buttonProps.buttonAction()))
                .setSize(100, 50);
    }

    private Coordinates getButtonCoords(ButtonProps buttonProps) {
        //TODO implement button placements
        int index = customButtons.size();
        return new Coordinates(coords.x(), coords.y());
    }

    private void getButtonAction(controlP5.ControlEvent e, ButtonAction buttonAction) {
        log.debug("Pressed button {}", e.getName());
        if (buttonAction != null) {
            buttonAction.doAction();
        }
        allButtonAction();
    }

    @Override
    protected void show() {
        super.show();
        closeButton.show();
        customButtons.forEach(Controller::show);
    }

    @Override
    protected void hide() {
        super.hide();
        closeButton.hide();
        customButtons.forEach(Controller::remove);
        customButtons.clear();
    }

    protected boolean onKeyAction(char key) {
        if (key == 'x') {
            allButtonAction();
            return true;
        }
        return false;
    }
}
