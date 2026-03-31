package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import lombok.Setter;

import static fi.monopoly.MonopolyApp.SPACE;
import static fi.monopoly.text.UiTexts.text;
import static fi.monopoly.utils.LayoutMetrics.popupButtonGapX;
import static fi.monopoly.utils.LayoutMetrics.popupButtonGapY;
import static fi.monopoly.utils.LayoutMetrics.popupButtonHeight;
import static fi.monopoly.utils.LayoutMetrics.popupButtonMaxWidth;
import static fi.monopoly.utils.LayoutMetrics.popupButtonMinWidth;
import static fi.monopoly.utils.LayoutMetrics.popupButtonPadding;
import static fi.monopoly.utils.LayoutMetrics.popupTextSidePadding;

public class ChoicePopup extends Popup {
    private final MonopolyButton acceptButton;
    private final MonopolyButton declineButton;

    @Setter
    private ButtonAction onAcceptAction;
    @Setter
    private ButtonAction onDeclineAction;

    protected ChoicePopup(MonopolyRuntime runtime) {
        this(runtime, "choice");
    }

    protected ChoicePopup(MonopolyRuntime runtime, String controlPrefix) {
        super(runtime);
        this.acceptButton = new MonopolyButton(runtime, controlPrefix + "Accept");
        acceptButton.addListener(this::acceptAction);
        acceptButton.setSize(popupButtonMinWidth(), popupButtonHeight());
        acceptButton.setAutoWidth(popupButtonMinWidth(), popupButtonPadding(), popupButtonMaxWidth());
        acceptButton.hide();
        this.declineButton = new MonopolyButton(runtime, controlPrefix + "Decline");
        declineButton.addListener(this::declineAction);
        declineButton.setSize(popupButtonMinWidth(), popupButtonHeight());
        declineButton.setAutoWidth(popupButtonMinWidth(), popupButtonPadding(), popupButtonMaxWidth());
        declineButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        acceptButton.setLabel(text("popup.choice.accept"));
        declineButton.setLabel(text("popup.choice.decline"));
    }

    private void acceptAction() {
        completeManualAction(onAcceptAction);
    }

    private void declineAction() {
        completeManualAction(onDeclineAction);
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
        if (key == '1' || key == SPACE) {
            acceptAction();
            return true;
        } else if (key == '2') {
            declineAction();
            return true;
        }
        return super.onKeyAction(key);
    }

    @Override
    protected boolean onComputerAction(ComputerPlayerProfile profile) {
        return triggerPrimaryAction(PopupActionTrigger.COMPUTER);
    }

    @Override
    public java.util.List<String> getVisibleActionLabels() {
        return java.util.List.of(text("popup.choice.accept"), text("popup.choice.decline"));
    }

    @Override
    protected void refreshControlLayout() {
        layoutButtons();
    }

    @Override
    protected boolean triggerPrimaryAction(PopupActionTrigger trigger) {
        completeAction(onAcceptAction, trigger);
        return true;
    }

    @Override
    protected boolean triggerSecondaryAction(PopupActionTrigger trigger) {
        completeAction(onDeclineAction, trigger);
        return true;
    }

    private void layoutButtons() {
        float centerX = getPopupCenter().x();
        float buttonY = getButtonAreaTop() + Math.max(0, (getButtonAreaHeight() - acceptButton.getHeight()) / 2f);
        float combinedWidth = acceptButton.getWidth() + declineButton.getWidth() + popupButtonGapX();
        float minInlineX = getPopupLeft() + popupTextSidePadding();
        float maxInlineRight = getPopupRight() - popupTextSidePadding();
        if (combinedWidth <= getPopupWidth() - popupTextSidePadding() * 2f) {
            float startX = centerX - combinedWidth / 2f;
            acceptButton.setPosition(Math.max(minInlineX, startX), buttonY);
            declineButton.setPosition(Math.min(maxInlineRight - declineButton.getWidth(), startX + acceptButton.getWidth() + popupButtonGapX()), buttonY);
            return;
        }

        float stackedX = centerX - Math.max(acceptButton.getWidth(), declineButton.getWidth()) / 2f;
        float firstY = getButtonAreaTop() + fi.monopoly.utils.LayoutMetrics.spacingXs();
        acceptButton.setPosition(stackedX, firstY);
        declineButton.setPosition(stackedX, firstY + acceptButton.getHeight() + popupButtonGapY());
    }
}
