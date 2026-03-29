package fi.monopoly.components.popup;

import controlP5.Button;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.utils.Coordinates;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;

@Slf4j
public class CustomPopup extends Popup {
    private static final int MIN_BUTTON_WIDTH = 100;
    private static final int MAX_BUTTON_WIDTH = 220;
    private final List<MonopolyButton> customButtons = new ArrayList<>();
    private int totalButtonCount = 0;
    private final Button closeButton;
    private final List<String> activeButtonLabels = new ArrayList<>();
    private static final int BUTTON_HEIGHT = 50;
    private static final int BUTTON_PADDING = 28;
    private static final int BUTTON_GAP_X = 12;
    private static final int BUTTON_GAP_Y = 12;

    protected CustomPopup(MonopolyRuntime runtime) {
        super(runtime);
        this.closeButton = new MonopolyButton(runtime, "close")
                .addListener(e -> completeAction(null))
                .hide()
                .setSize(20, 20);
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        closeButton.setLabel(text("popup.close.label"));
    }

    public void setButtons(ButtonProps... buttonProps) {
        totalButtonCount = buttonProps.length;
        activeButtonLabels.clear();
        ensureButtonPoolSize(buttonProps.length);
        for (int i = 0; i < buttonProps.length; i++) {
            configureButton(customButtons.get(i), buttonProps[i]);
            activeButtonLabels.add(buttonProps[i].name());
        }
        layoutButtons();
        for (int i = buttonProps.length; i < customButtons.size(); i++) {
            customButtons.get(i).hide();
        }
    }

    private void ensureButtonPoolSize(int requiredCount) {
        while (customButtons.size() < requiredCount) {
            MonopolyButton button = new MonopolyButton(runtime, "customButton" + customButtons.size());
            button.setSize(MIN_BUTTON_WIDTH, BUTTON_HEIGHT);
            button.setAutoWidth(MIN_BUTTON_WIDTH, BUTTON_PADDING, MAX_BUTTON_WIDTH);
            button.hide();
            customButtons.add(button);
        }
    }

    private void configureButton(MonopolyButton button, ButtonProps buttonProps) {
        button.setLabel(buttonProps.name());
        button.addListener(() -> getButtonAction(buttonProps.buttonAction()));
        button.setSize(MIN_BUTTON_WIDTH, BUTTON_HEIGHT);
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
        int totalHeight = rows * BUTTON_HEIGHT + Math.max(0, rows - 1) * BUTTON_GAP_Y;
        int startY = top + Math.max(0, Math.round((getButtonAreaHeight() - totalHeight) / 2f));

        for (int row = 0; row < rows; row++) {
            int rowStart = row * cols;
            int rowEnd = Math.min(rowStart + cols, totalButtonCount);
            int rowWidth = 0;
            for (int i = rowStart; i < rowEnd; i++) {
                rowWidth += Math.round(customButtons.get(i).getWidth());
            }
            rowWidth += Math.max(0, rowEnd - rowStart - 1) * BUTTON_GAP_X;
            int startX = Math.round(getPopupCenter().x() - rowWidth / 2f);
            int x = startX;
            int y = startY + row * (BUTTON_HEIGHT + BUTTON_GAP_Y);
            for (int i = rowStart; i < rowEnd; i++) {
                MonopolyButton button = customButtons.get(i);
                button.setPosition(new Coordinates(x, y));
                x += Math.round(button.getWidth()) + BUTTON_GAP_X;
            }
        }
    }

    private void getButtonAction(ButtonAction buttonAction) {
        completeAction(buttonAction);
    }

    @Override
    protected void show() {
        super.show();
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
    }

    @Override
    protected void refreshControlLayout() {
        layoutButtons();
    }

    protected boolean onKeyAction(char key) {
        char normalizedKey = Character.toLowerCase(key);
        if (normalizedKey == 'x') {
            completeAction(null);
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
        if (totalButtonCount > 0) {
            customButtons.get(0).pressButton();
            return true;
        }
        completeAction(null);
        return true;
    }

    @Override
    public List<String> getVisibleActionLabels() {
        return List.copyOf(activeButtonLabels);
    }

    private int getMaxButtonColumns() {
        return getPopupWidth() < 420 ? 2 : 3;
    }

    private void layoutCloseButton() {
        closeButton.setPosition(getPopupRight() - 30, getPopupTop() + 10);
    }
}
