package fi.monopoly.components;

import controlP5.Button;
import controlP5.CColor;
import fi.monopoly.client.desktop.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.popup.ButtonAction;
import fi.monopoly.utils.Coordinates;
import fi.monopoly.utils.MonopolyUtils;

public class MonopolyButton extends Button {
    private static final int DISABLED_BACKGROUND = 0xff9f9f9f;
    private static final int DISABLED_FOREGROUND = 0xff9f9f9f;
    private static final int DISABLED_ACTIVE = 0xff9f9f9f;
    private final MonopolyRuntime runtime;
    private ButtonAction buttonAction;
    private boolean allowedDuringComputerTurn = false;
    private Integer autoMinWidth;
    private Integer autoPadding;
    private Integer autoMaxWidth;
    private int enabledBackgroundColor;
    private int enabledForegroundColor;
    private int enabledActiveColor;
    private int currentBackgroundColor;
    private int currentForegroundColor;
    private int currentActiveColor;

    public MonopolyButton(MonopolyRuntime runtime, String id) {
        super(runtime.controlP5(), id);
        this.runtime = runtime;
        setFont(runtime.font20());
        CColor defaultColors = getColor();
        enabledBackgroundColor = defaultColors.getBackground();
        enabledForegroundColor = defaultColors.getForeground();
        enabledActiveColor = defaultColors.getActive();
        currentBackgroundColor = enabledBackgroundColor;
        currentForegroundColor = enabledForegroundColor;
        currentActiveColor = enabledActiveColor;
        applyColors(enabledBackgroundColor, enabledForegroundColor, enabledActiveColor);
        addListener(e -> {
            triggerManualAction();
        });
    }

    public MonopolyButton setPosition(Coordinates coordinates) {
        setPosition(coordinates.x(), coordinates.y());
        return this;
    }

    @Override
    public MonopolyButton setPosition(float x, float y) {
        float[] currentPosition = getPosition();
        if (currentPosition[0] == x && currentPosition[1] == y) {
            return this;
        }
        super.setPosition(x, y);
        return this;
    }

    public MonopolyButton setLabel(String label) {
        this.getCaptionLabel().setText(MonopolyUtils.parseIllegalCharacters(label));
        applyAutoWidth();
        refreshInteractivityStyle();
        return this;
    }

    public MonopolyButton addListener(ButtonAction buttonAction) {
        this.buttonAction = buttonAction;
        return this;
    }

    @Override
    public MonopolyButton setSize(int width, int height) {
        super.setSize(width, height);
        applyAutoWidth();
        return this;
    }

    public MonopolyButton setAutoWidth(int minWidth, int padding, int maxWidth) {
        this.autoMinWidth = minWidth;
        this.autoPadding = padding;
        this.autoMaxWidth = maxWidth;
        applyAutoWidth();
        return this;
    }

    public void pressButton() {
        triggerManualAction();
    }

    public MonopolyButton setAllowedDuringComputerTurn(boolean allowedDuringComputerTurn) {
        this.allowedDuringComputerTurn = allowedDuringComputerTurn;
        refreshInteractivityStyle();
        return this;
    }

    private void triggerManualAction() {
        refreshInteractivityStyle();
        if (!isManualActionAllowed()) {
            return;
        }
        if (buttonAction != null) {
            buttonAction.doAction();
        }
    }

    private boolean isManualActionAllowed() {
        if (MonopolyApp.DEBUG_MODE) {
            return true;
        }
        var session = runtime.gameSessionOrNull();
        Player turnPlayer = session != null && session.players() != null ? session.players().getTurn() : null;
        return allowedDuringComputerTurn || turnPlayer == null || !turnPlayer.isComputerControlled();
    }

    public MonopolyButton setButtonColors(int background, int foreground, int active) {
        enabledBackgroundColor = background;
        enabledForegroundColor = foreground;
        enabledActiveColor = active;
        refreshInteractivityStyle();
        return this;
    }

    public int currentBackgroundColor() {
        return currentBackgroundColor;
    }

    public int currentForegroundColor() {
        return currentForegroundColor;
    }

    public int currentActiveColor() {
        return currentActiveColor;
    }

    @Override
    public MonopolyButton show() {
        if (!isVisible()) {
            super.show();
        }
        refreshInteractivityStyle();
        return this;
    }

    @Override
    public MonopolyButton hide() {
        if (isVisible()) {
            super.hide();
        }
        return this;
    }

    public void dispose() {
        hide();
        remove();
    }

    public void refreshInteractivityStyle() {
        if (isManualActionAllowed()) {
            applyColors(enabledBackgroundColor, enabledForegroundColor, enabledActiveColor);
            return;
        }
        applyColors(DISABLED_BACKGROUND, DISABLED_FOREGROUND, DISABLED_ACTIVE);
    }

    private void applyColors(int background, int foreground, int active) {
        currentBackgroundColor = background;
        currentForegroundColor = foreground;
        currentActiveColor = active;
        setColorBackground(background);
        setColorForeground(foreground);
        setColorActive(active);
    }

    private void applyAutoWidth() {
        if (autoMinWidth == null || getHeight() <= 0) {
            return;
        }
        String label = getCaptionLabel().getText();
        if (label == null || label.isBlank()) {
            return;
        }
        float textWidth;
        try {
            runtime.app().pushStyle();
            runtime.app().textFont(runtime.font20());
            textWidth = runtime.app().textWidth(label);
            runtime.app().popStyle();
        } catch (RuntimeException e) {
            // Headless tests may not have an initialized graphics context yet.
            textWidth = label.length() * 12f;
        }
        int preferredWidth = Math.round(textWidth) + autoPadding;
        int clampedWidth = Math.max(autoMinWidth, Math.min(autoMaxWidth, preferredWidth));
        super.setSize(clampedWidth, getHeight());
    }
}
