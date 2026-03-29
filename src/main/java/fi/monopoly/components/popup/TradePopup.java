package fi.monopoly.components.popup;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.components.ButtonProps;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.CORNER;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;

public class TradePopup extends Popup {
    private static final int MIN_BUTTON_WIDTH = 100;
    private static final int MAX_BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 50;
    private static final int BUTTON_PADDING = 28;
    private static final int BUTTON_GAP_X = 12;
    private static final int BUTTON_GAP_Y = 12;
    private static final float PANELS_TOP_OFFSET = 74f;
    private static final float PANELS_BOTTOM_PADDING = 18f;
    private static final float PANEL_GAP = 18f;
    private static final float PANEL_PADDING = 14f;
    private static final float PANEL_HEADER_HEIGHT = 28f;
    private static final float CHIP_HEIGHT = 28f;
    private static final float CHIP_GAP_X = 8f;
    private static final float CHIP_GAP_Y = 8f;
    private static final float SUBTITLE_TOP_OFFSET = 38f;
    private static final float FOOTER_BOTTOM_PADDING = 22f;
    private final List<MonopolyButton> customButtons = new ArrayList<>();
    private final List<String> activeButtonLabels = new ArrayList<>();
    private final MonopolyButton closeButton;
    private int totalButtonCount = 0;
    private TradePopupView tradeView;

    protected TradePopup(MonopolyRuntime runtime) {
        super(runtime);
        this.closeButton = new MonopolyButton(runtime, "tradeClose")
                .addListener(() -> completeAction(null))
                .setSize(20, 20);
        closeButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        closeButton.setLabel(text("popup.close.label"));
    }

    public void setTradeView(TradePopupView tradeView) {
        this.tradeView = tradeView;
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
            MonopolyButton button = new MonopolyButton(runtime, "tradeCustomButton" + customButtons.size());
            button.setSize(MIN_BUTTON_WIDTH, BUTTON_HEIGHT);
            button.setAutoWidth(MIN_BUTTON_WIDTH, BUTTON_PADDING, MAX_BUTTON_WIDTH);
            button.hide();
            customButtons.add(button);
        }
    }

    private void configureButton(MonopolyButton button, ButtonProps buttonProps) {
        button.setLabel(buttonProps.name());
        button.addListener(() -> completeAction(buttonProps.buttonAction()));
        button.setSize(MIN_BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    @Override
    public void draw(PGraphics p) {
        if (!isVisible || tradeView == null) {
            return;
        }
        refreshControlLayout();
        float width = getPopupWidth();
        float height = getPopupHeight();
        float centerX = getPopupCenter().x();
        float centerY = getPopupCenter().y();

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();
        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(centerX, centerY, width, height, 30);
        p.popStyle();
        p.popMatrix();

        drawTradePanels(p);
    }

    private void drawTradePanels(PGraphics p) {
        float left = getPopupLeft();
        float top = getPopupTop();
        float width = getPopupWidth();
        float panelAreaTop = top + PANELS_TOP_OFFSET;
        float panelAreaHeight = getButtonAreaTop() - panelAreaTop - PANELS_BOTTOM_PADDING;
        float panelWidth = (width - TEXT_SIDE_PADDING * 2f - PANEL_GAP) / 2f;
        float panelHeight = Math.max(100f, panelAreaHeight);
        float leftPanelX = left + TEXT_SIDE_PADDING;
        float rightPanelX = leftPanelX + panelWidth + PANEL_GAP;

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, TOP);
        p.text(tradeView.title(), getPopupCenter().x(), top + TEXT_TOP_OFFSET);
        if (tradeView.subtitle() != null && !tradeView.subtitle().isBlank()) {
            p.textFont(runtime.font20());
            p.text(tradeView.subtitle(), getPopupCenter().x(), top + SUBTITLE_TOP_OFFSET);
        }

        drawPanel(p, leftPanelX, panelAreaTop, panelWidth, panelHeight, tradeView.leftPlayerName(), tradeView.leftItems(), tradeView.highlightLeft());
        drawPanel(p, rightPanelX, panelAreaTop, panelWidth, panelHeight, tradeView.rightPlayerName(), tradeView.rightItems(), tradeView.highlightRight());

        if (tradeView.footer() != null && !tradeView.footer().isBlank()) {
            p.fill(0);
            p.textFont(runtime.font20());
            p.textAlign(CENTER, TOP);
            p.text(tradeView.footer(), getPopupCenter().x(), panelAreaTop + panelHeight - FOOTER_BOTTOM_PADDING);
        }

        p.popStyle();
        p.popMatrix();
    }

    private void drawPanel(PGraphics p, float x, float y, float width, float height, String title, List<String> items, boolean highlighted) {
        p.rectMode(CORNER);
        p.stroke(highlighted ? p.color(214, 112, 26) : p.color(60));
        p.strokeWeight(highlighted ? 4 : 2);
        p.fill(highlighted ? p.color(255, 241, 207) : p.color(247, 236, 205));
        p.rect(x, y, width, height, 18);

        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(LEFT, TOP);
        p.text(title, x + PANEL_PADDING, y + PANEL_PADDING);

        float chipX = x + PANEL_PADDING;
        float chipY = y + PANEL_PADDING + PANEL_HEADER_HEIGHT;
        float chipMaxX = x + width - PANEL_PADDING;

        for (String item : items) {
            float chipWidth = measureChipWidth(p, item);
            if (chipX + chipWidth > chipMaxX) {
                chipX = x + PANEL_PADDING;
                chipY += CHIP_HEIGHT + CHIP_GAP_Y;
            }
            drawChip(p, chipX, chipY, chipWidth, item, highlighted);
            chipX += chipWidth + CHIP_GAP_X;
        }
    }

    private void drawChip(PGraphics p, float x, float y, float width, String label, boolean highlighted) {
        p.rectMode(CORNER);
        p.stroke(highlighted ? p.color(214, 112, 26) : p.color(120));
        p.strokeWeight(1.5f);
        p.fill(highlighted ? p.color(255, 221, 170) : p.color(255));
        p.rect(x, y, width, CHIP_HEIGHT, 12);
        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, CENTER);
        p.text(label, x + width / 2f, y + CHIP_HEIGHT / 2f - 2f);
    }

    private float measureChipWidth(PGraphics p, String label) {
        p.textFont(runtime.font20());
        return Math.max(64f, p.textWidth(label) + 18f);
    }

    private void layoutButtons() {
        layoutCloseButton();
        if (totalButtonCount == 0) {
            return;
        }
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
            int rowY = startY + row * (BUTTON_HEIGHT + BUTTON_GAP_Y);
            int buttonX = startX;
            for (int i = rowStart; i < rowEnd; i++) {
                MonopolyButton button = customButtons.get(i);
                button.setPosition(buttonX, rowY);
                buttonX += Math.round(button.getWidth()) + BUTTON_GAP_X;
            }
        }
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
        activeButtonLabels.clear();
        totalButtonCount = 0;
        tradeView = null;
    }

    @Override
    protected void refreshControlLayout() {
        layoutButtons();
    }

    @Override
    protected boolean onKeyAction(char key) {
        char normalizedKey = Character.toLowerCase(key);
        if (normalizedKey == 'x') {
            completeAction(null);
            return true;
        }
        try {
            int index = Integer.parseInt(String.valueOf(key)) - 1;
            if (index >= 0 && index < totalButtonCount) {
                customButtons.get(index).pressButton();
                return true;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    @Override
    protected boolean onComputerAction(ComputerPlayerProfile profile) {
        return triggerPrimaryAction();
    }

    @Override
    public List<String> getVisibleActionLabels() {
        return List.copyOf(activeButtonLabels);
    }

    @Override
    protected boolean triggerPrimaryAction() {
        if (totalButtonCount > 0) {
            customButtons.get(0).pressButton();
            return true;
        }
        completeAction(null);
        return true;
    }

    @Override
    protected boolean triggerSecondaryAction() {
        completeAction(null);
        return true;
    }

    private int getMaxButtonColumns() {
        return getPopupWidth() < 420 ? 2 : 3;
    }

    private void layoutCloseButton() {
        closeButton.setPosition(getPopupRight() - 30, getPopupTop() + 10);
    }
}
