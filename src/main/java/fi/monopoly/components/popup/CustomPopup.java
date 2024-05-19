package fi.monopoly.components.popup;

import controlP5.Button;
import controlP5.Controller;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.utils.Coordinates;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomPopup extends Popup {
    private List<MonopolyButton> customButtons = new ArrayList<>();
    private int totalButtonCount = 0;
    private final Button closeButton = new MonopolyButton("close")
            .setPosition(coords.x() + (float) width / 2 - (float) width / 10, coords.y() - (float) height / 2 + (float) height / 20)
            .addListener(e -> allButtonAction())
            .setLabel("X")
            .hide()
            .setSize(30, 30);
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 50;

    public void setButtons(ButtonProps... buttonProps) {
        totalButtonCount = buttonProps.length;
        customButtons = new ArrayList<>();
        for (ButtonProps buttonProp : buttonProps) {
            customButtons.add(getButton(buttonProp));
        }
    }

    private MonopolyButton getButton(ButtonProps buttonProps) {
        MonopolyButton button = new MonopolyButton("customButton" + customButtons.size())
                .setLabel(buttonProps.name())
                .setPosition(getButtonCoords())
                .addListener(() -> getButtonAction(buttonProps.buttonAction()));
        button.setSize(BUTTON_WIDTH, BUTTON_HEIGHT);

        return button;
    }

    /**
     * Method straigt from chatgbt :)
     *
     * @return button coordinates
     */
    private Coordinates getButtonCoords() {
        int index = customButtons.size();
        int n = totalButtonCount;
        // Calculate the number of columns and rows needed to arrange the buttons
        int cols = (int) Math.ceil(Math.sqrt(n));  // Number of columns in the grid
        int rows = (int) Math.ceil((double) n / cols);  // Number of rows in the grid

        // Calculate the width and height of each cell in the grid
        int cellWidth = width / cols;  // Width of each cell
        int cellHeight = (int) ((height - BUTTON_HEIGHT * 1.5) / rows);  // Height of each cell

        // Determine the row and column of the current button based on its index
        int row = index / cols;  // Row position of the button
        int col = index % cols;  // Column position of the button

        // Calculate the top-left position of the cell for the button
        int cellCenterX = col * cellWidth + cellWidth / 2;  // X-coordinate of the cell's center
        int cellCenterY = row * cellHeight + cellHeight / 2;  // Y-coordinate of the cell's center

        // Calculate the top-left position of the button relative to the cell center
        int x = cellCenterX - BUTTON_WIDTH / 2;  // Top-left X-coordinate of the button
        int y = cellCenterY - BUTTON_HEIGHT / 2;  // Top-left Y-coordinate of the button

        return new Coordinates(x, y).move(coords.x() / 2, (float) (coords.y() * 0.7 + BUTTON_HEIGHT * 1.4));  // Return the coordinates as a Dimension object

    }

    private void getButtonAction(ButtonAction buttonAction) {
        allButtonAction();
        if (buttonAction != null) {
            buttonAction.doAction();
        }
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
        totalButtonCount = 0;
        closeButton.hide();
        customButtons.forEach(Controller::remove);
        customButtons.clear();
    }

    protected boolean onKeyAction(char key) {
        if (key == 'x') {
            allButtonAction();
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
}
