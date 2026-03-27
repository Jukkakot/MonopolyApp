package fi.monopoly.components.popup;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import lombok.Setter;

import static fi.monopoly.text.UiTexts.text;

public class ChoicePopup extends Popup {
    private final MonopolyButton acceptButton;
    private final MonopolyButton declineButton;

    @Setter
    private ButtonAction onAcceptAction;
    @Setter
    private ButtonAction onDeclineAction;

    protected ChoicePopup(MonopolyRuntime runtime) {
        super(runtime);
        this.acceptButton = new MonopolyButton(runtime, "accept");
        acceptButton.setPosition(coords.x() - 150, coords.y() + (float) height / 4);
        acceptButton.addListener(this::acceptAction);
        acceptButton.setSize(100, 50);
        acceptButton.setAutoWidth(100, 28, 180);
        acceptButton.hide();
        this.declineButton = new MonopolyButton(runtime, "decline");
        declineButton.setPosition(coords.x() + 50, coords.y() + (float) height / 4);
        declineButton.addListener(this::declineAction);
        declineButton.setSize(100, 50);
        declineButton.setAutoWidth(100, 28, 180);
        declineButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        acceptButton.setLabel(text("popup.choice.accept"));
        declineButton.setLabel(text("popup.choice.decline"));
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
