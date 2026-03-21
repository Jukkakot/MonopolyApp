package fi.monopoly.components.popup;

import controlP5.Button;
import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import lombok.Setter;

public class ChoicePopup extends Popup {
    private final Button acceptButton;
    private final Button declineButton;

    @Setter
    private ButtonAction onAcceptAction;
    @Setter
    private ButtonAction onDeclineAction;

    protected ChoicePopup(MonopolyRuntime runtime) {
        super(runtime);
        this.acceptButton = new MonopolyButton(runtime, "accept")
                .setPosition(coords.x() - 150, coords.y() + (float) height / 4)
                .addListener(e -> acceptAction())
                .setLabel("Accept")
                .hide()
                .setSize(100, 50);
        this.declineButton = new MonopolyButton(runtime, "decline")
                .setPosition(coords.x() + 50, coords.y() + (float) height / 4)
                .addListener(e -> declineAction())
                .setLabel("Decline")
                .hide()
                .setSize(100, 50);
    }

    private void acceptAction() {
        completeAction(onAcceptAction);
    }

    private void declineAction() {
        completeAction(onDeclineAction);
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
