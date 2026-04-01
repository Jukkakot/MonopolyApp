package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.UiTokens;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class CustomPopup extends Popup {
    private final List<MonopolyButton> customButtons = new ArrayList<>();
    private int totalButtonCount = 0;
    private final MonopolyButton closeButton;
    private final List<String> activeButtonLabels = new ArrayList<>();
    private final List<ButtonAction> activeButtonActions = new ArrayList<>();
    private boolean computerResolvable = true;
    private boolean manualInteractionDuringComputerTurn;

    protected CustomPopup(MonopolyRuntime runtime) {
        super(runtime);
        this.closeButton = new MonopolyButton(runtime, "close");
        closeButton.addListener(() -> completeManualAction(null));
        closeButton.hide();
        closeButton.setSize(UiTokens.popupCloseButtonSize(), UiTokens.popupCloseButtonSize());
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        closeButton.setLabel(text("popup.close.label"));
    }

    public void setButtons(ButtonProps... buttonProps) {
        totalButtonCount = buttonProps.length;
        activeButtonLabels.clear();
        activeButtonActions.clear();
        ensureButtonPoolSize(buttonProps.length);
        for (int i = 0; i < buttonProps.length; i++) {
            configureButton(customButtons.get(i), buttonProps[i]);
            activeButtonLabels.add(buttonProps[i].name());
            activeButtonActions.add(buttonProps[i].buttonAction());
        }
        layoutButtons();
        for (int i = buttonProps.length; i < customButtons.size(); i++) {
            customButtons.get(i).hide();
        }
    }

    private void ensureButtonPoolSize(int requiredCount) {
        while (customButtons.size() < requiredCount) {
            MonopolyButton button = new MonopolyButton(runtime, "customButton" + customButtons.size());
            button.setSize(UiTokens.popupButtonMinWidth(), UiTokens.popupButtonHeight());
            button.setAutoWidth(
                    UiTokens.popupButtonMinWidth(),
                    UiTokens.popupButtonPadding(),
                    UiTokens.popupButtonMaxWidth()
            );
            button.hide();
            customButtons.add(button);
        }
    }

    private void configureButton(MonopolyButton button, ButtonProps buttonProps) {
        button.setLabel(buttonProps.name());
        button.addListener(() -> getButtonAction(buttonProps.buttonAction()));
        button.setSize(UiTokens.popupButtonMinWidth(), UiTokens.popupButtonHeight());
        button.setAllowedDuringComputerTurn(manualInteractionDuringComputerTurn);
    }

    private void layoutButtons() {
        if (totalButtonCount == 0) {
            layoutCloseButton();
            return;
        }
        layoutCloseButton();
        int cols = Math.min(getMaxButtonColumns(), (int) Math.ceil(Math.sqrt(totalButtonCount)));
        int rows = (int) Math.ceil((double) totalButtonCount / cols);
        int top = Math.round(getButtonAreaTop());
        int totalHeight = rows * UiTokens.popupButtonHeight()
                + Math.max(0, rows - 1) * UiTokens.popupButtonGapY();
        int startY = top + Math.max(0, Math.round((getButtonAreaHeight() - totalHeight) / 2f));

        for (int row = 0; row < rows; row++) {
            int rowStart = row * cols;
            int rowEnd = Math.min(rowStart + cols, totalButtonCount);
            int rowWidth = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                rowWidth += Math.round(customButtons.get(i).getWidth());
            }
            rowWidth += Math.max(0, rowEnd - rowStart - 1) * UiTokens.popupButtonGapX();
            int startX = Math.round(getPopupCenter().x() - rowWidth / 2f);
            int x = startX;
            int y = startY + row * (UiTokens.popupButtonHeight() + UiTokens.popupButtonGapY());
            for (int i = rowStart; i < rowEnd; i++) {
                MonopolyButton button = customButtons.get(i);
                button.setPosition(new Coordinates(x, y));
                x += Math.round(button.getWidth()) + UiTokens.popupButtonGapX();
            }
        }
    }

    private void getButtonAction(ButtonAction buttonAction) {
        completeManualAction(buttonAction);
    }

    @Override
    protected void show() {
        super.show();
        closeButton.setAllowedDuringComputerTurn(manualInteractionDuringComputerTurn);
        layoutButtons();
        closeButton.show();
        for (int i = 0; i < totalButtonCount; i++) {
            customButtons.get(i).show();
        }
    }

    @Override
    protected void hide() {
        super.hide();
        closeButton.hide();
        customButtons.forEach(MonopolyButton::hide);
        totalButtonCount = 0;
        activeButtonLabels.clear();
        activeButtonActions.clear();
        computerResolvable = true;
        manualInteractionDuringComputerTurn = false;
    }

    @Override
    protected void refreshControlLayout() {
        layoutButtons();
    }

    protected boolean onKeyAction(char key) {
        char normalizedKey = Character.toLowerCase(key);
        if (normalizedKey == 'x') {
            completeManualAction(null);
            return true;
        }
        try {
            int index = Integer.parseInt(String.valueOf(key)) - 1;
            if (index >= 0 && index < customButtons.size()) {
                customButtons.get(index).pressButton();
            }
        } catch (NumberFormatException e) {
        }

        return super.onKeyAction(key);
    }

    @Override
    protected boolean onComputerAction(ComputerPlayerProfile profile) {
        if (!computerResolvable) {
            return false;
        }
        return triggerPrimaryAction(PopupActionTrigger.COMPUTER);
    }

    @Override
    public List<String> getVisibleActionLabels() {
        return List.copyOf(activeButtonLabels);
    }

    @Override
    protected boolean triggerPrimaryAction(PopupActionTrigger trigger) {
        if (totalButtonCount > 0) {
            completeAction(getButtonActionAt(0), trigger);
            return true;
        }
        completeAction(null, trigger);
        return true;
    }

    @Override
    protected boolean triggerSecondaryAction(PopupActionTrigger trigger) {
        completeAction(null, trigger);
        return true;
    }

    public void setInteractionPolicy(boolean computerResolvable, boolean manualInteractionDuringComputerTurn) {
        this.computerResolvable = computerResolvable;
        this.manualInteractionDuringComputerTurn = manualInteractionDuringComputerTurn;
    }

    private ButtonAction getButtonActionAt(int index) {
        if (index < 0 || index >= totalButtonCount) {
            return null;
        }
        return activeButtonActions.get(index);
    }

    private int getMaxButtonColumns() {
        return getPopupWidth() < 420 ? 2 : 3;
    }

    private void layoutCloseButton() {
        closeButton.setPosition(
                getPopupRight() - UiTokens.popupCloseButtonRightInset(),
                getPopupTop() + UiTokens.popupCloseButtonTopInset()
        );
    }

    @Override
    protected boolean allowManualInteractionDuringComputerTurn() {
        return manualInteractionDuringComputerTurn;
    }
}
