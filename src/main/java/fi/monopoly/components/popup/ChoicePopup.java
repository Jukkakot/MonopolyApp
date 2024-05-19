package fi.monopoly.components.popup;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.components.MonopolyButton;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChoicePopup extends Popup {

    private final Button acceptButton = new MonopolyButton("accept")
            .setPosition(coords.x() - 150, coords.y() + (float) height / 4)
            .addListener(e -> acceptAction())
            .setLabel("Accept")
            .hide()
            .setSize(100, 50);
    private final Button declineButton = new MonopolyButton("decline")
            .setPosition(coords.x() + 50, coords.y() + (float) height / 4)
            .addListener(e -> declineAction())
            .setLabel("Decline")
            .hide()
            .setSize(100, 50);
    @Setter
    private ButtonAction onAcceptAction;
    @Setter
    private ButtonAction onDeclineAction;

    private void acceptAction() {
        if (onAcceptAction != null) {
            onAcceptAction.doAction();
        }
        allButtonAction();
    }

    private void declineAction() {
        if (onDeclineAction != null) {
            onDeclineAction.doAction();
        }
        allButtonAction();
    }

    @Override
    public void show() {
        super.show();
        acceptButton.show();
        declineButton.show();
    }

    @Override
    protected void hide() {
        super.hide();
        acceptButton.hide();
        declineButton.hide();
        onAcceptAction = null;
        onDeclineAction = null;
    }

    @Override
    protected boolean onKeyAction(char key) {
        if (key == '1' || (MonopolyApp.SKIP_ANNIMATIONS && key == ' ')) {
            acceptAction();
            return true;
        } else if (key == '2') {
            declineAction();
            return true;
        }
        return super.onKeyAction(key);
    }
}
