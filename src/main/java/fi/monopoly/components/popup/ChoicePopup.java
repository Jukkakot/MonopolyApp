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
        acceptButton.addListener(this::acceptAction);
        acceptButton.setSize(100, 50);
        acceptButton.setAutoWidth(100, 28, 180);
        acceptButton.hide();
        this.declineButton = new MonopolyButton(runtime, "decline");
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
        layoutButtons();
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

    private void layoutButtons() {
        float centerX = getPopupCenter().x();
        float buttonY = getButtonAreaTop() + Math.max(0, (getButtonAreaHeight() - acceptButton.getHeight()) / 2f);
        float combinedWidth = acceptButton.getWidth() + declineButton.getWidth() + 16f;
        float minInlineX = getPopupLeft() + 20f;
        float maxInlineRight = getPopupRight() - 20f;
        if (combinedWidth <= getPopupWidth() - 40f) {
            float startX = centerX - combinedWidth / 2f;
            acceptButton.setPosition(Math.max(minInlineX, startX), buttonY);
            declineButton.setPosition(Math.min(maxInlineRight - declineButton.getWidth(), startX + acceptButton.getWidth() + 16f), buttonY);
            return;
        }

        float stackedX = centerX - Math.max(acceptButton.getWidth(), declineButton.getWidth()) / 2f;
        float firstY = getButtonAreaTop() + 8f;
        acceptButton.setPosition(stackedX, firstY);
        declineButton.setPosition(stackedX, firstY + acceptButton.getHeight() + 12f);
    }
}
