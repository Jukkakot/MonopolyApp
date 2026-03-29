package fi.monopoly.components.popup;

import fi.monopoly.MonopolyApp;
import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.MonopolyButton;
import fi.monopoly.components.Player;
import fi.monopoly.components.PlayerToken;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.popup.components.ButtonProps;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.StreetType;
import fi.monopoly.utils.MonopolyUtils;
import javafx.scene.paint.Color;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.List;

import static fi.monopoly.text.UiTexts.text;
import static processing.core.PConstants.CENTER;
import static processing.core.PConstants.CORNER;
import static processing.core.PConstants.LEFT;
import static processing.core.PConstants.TOP;
import static processing.event.MouseEvent.CLICK;
import static processing.event.MouseEvent.MOVE;

public class TradePopup extends Popup {
    private static final int POPUP_WIDTH = 980;
    private static final int POPUP_HEIGHT = 820;
    private static final int MIN_BUTTON_WIDTH = 100;
    private static final int MAX_BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 50;
    private static final int BUTTON_PADDING = 28;
    private static final int BUTTON_GAP_X = 12;
    private static final int BUTTON_GAP_Y = 12;
    private static final float PANELS_TOP_OFFSET = 82f;
    private static final float PANELS_HEIGHT = 240f;
    private static final float PANEL_GAP = 18f;
    private static final float PANEL_PADDING = 14f;
    private static final float PANEL_HEADER_HEIGHT = 46f;
    private static final float ITEM_GAP = 10f;
    private static final float INVENTORY_TOP_GAP = 18f;
    private static final float INVENTORY_TITLE_GAP = 28f;
    private static final float INVENTORY_CARD_W = Spot.SPOT_W;
    private static final float INVENTORY_CARD_H = Spot.SPOT_H;
    private static final float SUMMARY_CARD_W = Spot.SPOT_W;
    private static final float SUMMARY_CARD_H = Spot.SPOT_H;
    private static final float TOKEN_SIZE = 34f;
    private static final float SUBTITLE_TOP_OFFSET = 40f;
    private static final float FOOTER_BOTTOM_PADDING = 22f;
    private static final float PROPERTY_COLOR_STRIPE_HEIGHT = 18f;
    private static final float BUTTON_AREA_BOTTOM_MARGIN = 28f;
    private static final float BUTTON_AREA_TOP_MARGIN = 18f;
    private static final float POPUP_TOP_MARGIN = 6f;
    private static final int CARD_BASE_R = 205;
    private static final int CARD_BASE_G = 230;
    private static final int CARD_BASE_B = 209;
    private static final int CARD_INSET = 4;
    private static final float CARD_RADIUS = 14f;
    private static final float CARD_INNER_RADIUS = 11f;

    private final List<MonopolyButton> customButtons = new ArrayList<>();
    private final List<String> activeButtonLabels = new ArrayList<>();
    private final List<ClickableRegion> clickableRegions = new ArrayList<>();
    private final MonopolyButton closeButton;
    private final MonopolyButton backButton;
    private int totalButtonCount = 0;
    private TradePopupView tradeView;
    private Object hoveredItemKey;

    protected TradePopup(MonopolyRuntime runtime) {
        super(runtime);
        this.closeButton = new MonopolyButton(runtime, "tradeClose")
                .addListener(() -> completeAction(null))
                .setSize(20, 20);
        this.backButton = new MonopolyButton(runtime, "tradeBack")
                .addListener(() -> {
                    if (totalButtonCount > 0) {
                        customButtons.get(0).pressButton();
                    }
                })
                .setSize(56, 32);
        closeButton.hide();
        backButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        closeButton.setLabel(text("popup.close.label"));
        backButton.setLabel("<-");
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

    @Override
    protected int getPopupWidth() {
        int availableWidth = Math.round(runtime.app().width) - WINDOW_MARGIN * 2;
        return Math.max(560, Math.min(POPUP_WIDTH, availableWidth));
    }

    @Override
    protected int getPopupHeight() {
        int availableHeight = runtime.app().height - WINDOW_MARGIN * 2;
        return Math.max(420, Math.min(POPUP_HEIGHT, availableHeight));
    }

    @Override
    protected fi.monopoly.utils.Coordinates getPopupCenter() {
        return fi.monopoly.utils.Coordinates.of(runtime.app().width / 2f, getPopupTop() + getPopupHeight() / 2f);
    }

    @Override
    protected float getPopupTop() {
        return POPUP_TOP_MARGIN;
    }

    @Override
    protected float getButtonAreaTop() {
        return getPopupBottom() - BUTTON_AREA_BOTTOM_MARGIN - requiredButtonAreaHeight();
    }

    @Override
    protected float getButtonAreaHeight() {
        return requiredButtonAreaHeight();
    }

    @Override
    public void draw(PGraphics p) {
        if (!isVisible || tradeView == null) {
            return;
        }
        refreshControlLayout();
        clickableRegions.clear();
        drawBackground(p);
        drawTradePanels(p);
        hoveredItemKey = resolveHoveredItemKey(runtime.app().mouseX, runtime.app().mouseY);
    }

    private void drawBackground(PGraphics p) {
        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();
        p.fill(p.color(255, 217, 127));
        p.rectMode(CENTER);
        p.stroke(0);
        p.strokeWeight(10);
        p.rect(getPopupCenter().x(), getPopupCenter().y(), getPopupWidth(), getPopupHeight(), 30);
        p.popStyle();
        p.popMatrix();
    }

    private void drawTradePanels(PGraphics p) {
        float left = getPopupLeft();
        float top = getPopupTop();
        float width = getPopupWidth();
        float panelWidth = (width - TEXT_SIDE_PADDING * 2f - PANEL_GAP) / 2f;
        float leftPanelX = left + TEXT_SIDE_PADDING;
        float rightPanelX = leftPanelX + panelWidth + PANEL_GAP;
        float panelY = top + PANELS_TOP_OFFSET;

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(0);
        p.textFont(runtime.font30());
        p.textAlign(CENTER, TOP);
        p.text(tradeView.title(), getPopupCenter().x(), top + TEXT_TOP_OFFSET);
        if (tradeView.subtitle() != null && !tradeView.subtitle().isBlank()) {
            p.textFont(runtime.font20());
            p.text(tradeView.subtitle(), getPopupCenter().x(), top + SUBTITLE_TOP_OFFSET + 4f);
        }

        drawPanel(p, leftPanelX, panelY, panelWidth, PANELS_HEIGHT, tradeView.leftPlayer(), tradeView.leftItems(), tradeView.highlightLeft());
        drawPanel(p, rightPanelX, panelY, panelWidth, PANELS_HEIGHT, tradeView.rightPlayer(), tradeView.rightItems(), tradeView.highlightRight());
        if (tradeView.leftAction() != null) {
            clickableRegions.add(new ClickableRegion(leftPanelX, panelY, panelWidth, PANELS_HEIGHT, tradeView.leftAction(), tradeView.leftPlayer()));
        }
        if (tradeView.rightAction() != null) {
            clickableRegions.add(new ClickableRegion(rightPanelX, panelY, panelWidth, PANELS_HEIGHT, tradeView.rightAction(), tradeView.rightPlayer()));
        }

        float inventoryTitleY = panelY + PANELS_HEIGHT + INVENTORY_TOP_GAP;
        if (tradeView.inventoryTitle() != null && !tradeView.inventoryTitle().isBlank()) {
            p.fill(0);
            p.textFont(runtime.font20());
            p.textAlign(LEFT, TOP);
            p.text(tradeView.inventoryTitle(), left + TEXT_SIDE_PADDING, inventoryTitleY);
        }
        drawInventory(p, left + TEXT_SIDE_PADDING, inventoryTitleY + INVENTORY_TITLE_GAP, width - TEXT_SIDE_PADDING * 2f, getButtonAreaTop() - (inventoryTitleY + INVENTORY_TITLE_GAP) - 12f);

        if (tradeView.footer() != null && !tradeView.footer().isBlank()) {
            p.fill(p.color(85, 42, 0));
            p.textFont(runtime.font20());
            p.textAlign(CENTER, TOP);
            p.text(tradeView.footer(), getPopupCenter().x(), getButtonAreaTop() - FOOTER_BOTTOM_PADDING - 6f);
        }

        p.popStyle();
        p.popMatrix();
    }

    private void drawPanel(PGraphics p, float x, float y, float width, float height, Player player, List<TradePopupItem> items, boolean highlighted) {
        p.rectMode(CORNER);
        p.stroke(highlighted ? p.color(214, 112, 26) : p.color(60));
        p.strokeWeight(highlighted ? 4 : 2);
        p.fill(highlighted ? p.color(255, 241, 207) : p.color(247, 236, 205));
        p.rect(x, y, width, height, 18);

        drawPlayerHeader(p, x, y, width, player, highlighted);

        float itemX = x + PANEL_PADDING;
        float itemY = y + PANEL_PADDING + PANEL_HEADER_HEIGHT;
        float maxX = x + width - PANEL_PADDING;
        for (TradePopupItem item : items) {
            float cardW = (item.type() == TradePopupItemType.PROPERTY || item.type() == TradePopupItemType.JAIL_CARD) ? SUMMARY_CARD_W : 112f;
            float cardH = (item.type() == TradePopupItemType.PROPERTY || item.type() == TradePopupItemType.JAIL_CARD) ? SUMMARY_CARD_H : 48f;
            if (itemX + cardW > maxX) {
                itemX = x + PANEL_PADDING;
                itemY += cardH + ITEM_GAP;
            }
            drawTradeItem(p, item, itemX, itemY, cardW, cardH, highlighted, false);
            itemX += cardW + ITEM_GAP;
        }
    }

    private void drawPlayerHeader(PGraphics p, float x, float y, float width, Player player, boolean highlighted) {
        if (player == null) {
            return;
        }
        float tokenX = x + PANEL_PADDING + TOKEN_SIZE / 2f;
        float tokenY = y + PANEL_PADDING + TOKEN_SIZE / 2f + 2f;
        drawPlayerToken(p, player, tokenX, tokenY, TOKEN_SIZE);
        p.fill(highlighted ? p.color(110, 60, 12) : p.color(0));
        p.textFont(runtime.font20());
        p.textAlign(LEFT, TOP);
        p.text(player.getName(), x + PANEL_PADDING + TOKEN_SIZE + 10f, y + PANEL_PADDING);
    }

    private void drawInventory(PGraphics p, float startX, float startY, float availableWidth, float availableHeight) {
        List<TradePopupItem> items = tradeView.inventoryItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        int columns = Math.max(1, Math.min(items.size(), (int) Math.floor((availableWidth + ITEM_GAP) / (INVENTORY_CARD_W + ITEM_GAP))));
        float x = startX;
        float y = startY;
        int indexInRow = 0;
        for (TradePopupItem item : items) {
            if (indexInRow >= columns) {
                indexInRow = 0;
                x = startX;
                y += INVENTORY_CARD_H + ITEM_GAP;
            }
            if (y + INVENTORY_CARD_H > startY + availableHeight) {
                break;
            }
            drawTradeItem(p, item, x, y, INVENTORY_CARD_W, INVENTORY_CARD_H, item.selected() || isHovered(item), true);
            if (item.action() != null) {
                clickableRegions.add(new ClickableRegion(
                        x,
                        y,
                        INVENTORY_CARD_W,
                        INVENTORY_CARD_H,
                        item.action(),
                        hoverKey(item)
                ));
            }
            x += INVENTORY_CARD_W + ITEM_GAP;
            indexInRow++;
        }
    }

    private void drawTradeItem(PGraphics p, TradePopupItem item, float x, float y, float width, float height, boolean highlighted, boolean showLabel) {
        drawCardFrame(p, x, y, width, height, highlighted, item.selected());

        switch (item.type()) {
            case PROPERTY -> drawPropertyItem(p, item, x, y, width, height, showLabel);
            case MONEY -> drawMoneyItem(p, item, x, y, width, height);
            case PLAYER -> drawPlayerItem(p, item, x, y, width, height);
            case JAIL_CARD -> drawJailCardItem(p, item, x, y, width, height);
            case EMPTY -> drawBadgeItem(p, item.label(), x, y, width, height);
        }
    }

    private void drawPropertyItem(PGraphics p, TradePopupItem item, float x, float y, float width, float height, boolean showLabel) {
        int stripeColor = resolveTradePropertyStripeColor(item.property());
        drawCardStripe(p, x, y, width, stripeColor);

        PImage image = MonopolyApp.getImage(item.property().getSpotType());
        if (image != null) {
            p.imageMode(CORNER);
            p.image(image, x + 8f, y + 8f + PROPERTY_COLOR_STRIPE_HEIGHT, width - 16f, height - (showLabel ? 42f : 16f) - PROPERTY_COLOR_STRIPE_HEIGHT);
        }
        p.fill(0);
        p.textFont(runtime.font10());
        p.textAlign(CENTER, TOP);
        p.textLeading(10);
        p.text(item.property().getDisplayName(), x + width * 0.12f, y + height - 30f, width * 0.76f, 24f);
    }

    private void drawJailCardItem(PGraphics p, TradePopupItem item, float x, float y, float width, float height) {
        PImage image = MonopolyApp.getImage("GetOutOfJail.png");
        if (image != null) {
            p.imageMode(CORNER);
            p.image(image, x + 8f, y + 8f, width - 16f, height - 40f);
        }
        p.fill(0);
        p.textFont(runtime.font10());
        p.textAlign(CENTER, TOP);
        p.textLeading(10);
        p.text(item.label(), x + width * 0.12f, y + height - 30f, width * 0.76f, 24f);
    }

    private void drawMoneyItem(PGraphics p, TradePopupItem item, float x, float y, float width, float height) {
        drawCardStripe(p, x, y, width, runtime.app().color(86, 142, 91));
        p.fill(runtime.app().color(41, 91, 48));
        p.textFont(runtime.font30());
        p.textAlign(CENTER, CENTER);
        p.text("M", x + width / 2f, y + height / 2f - 10f);
        p.fill(0);
        p.textFont(runtime.font20());
        p.text(item.label(), x + width / 2f, y + height - 24f);
    }

    private void drawPlayerItem(PGraphics p, TradePopupItem item, float x, float y, float width, float height) {
        if (item.player() != null) {
            drawPlayerToken(p, item.player(), x + width / 2f, y + 44f, 42f);
            p.fill(0);
            p.textFont(runtime.font10());
            p.textAlign(CENTER, TOP);
            p.text(item.player().getName(), x + width / 2f, y + 78f);
        }
    }

    private void drawBadgeItem(PGraphics p, String label, float x, float y, float width, float height) {
        p.fill(0);
        p.textFont(runtime.font20());
        p.textAlign(CENTER, CENTER);
        p.text(label, x + width / 2f, y + height / 2f);
    }

    private void drawPlayerToken(PGraphics p, Player player, float centerX, float centerY, float size) {
        PImage tokenImage = MonopolyApp.getImage("Token.png", player.getColor());
        if (tokenImage != null) {
            p.imageMode(CENTER);
            p.image(tokenImage, centerX, centerY, size, size);
            return;
        }
        int colorValue = MonopolyUtils.toColor(runtime.app(), player.getColor());
        p.fill(colorValue);
        p.stroke(0);
        p.strokeWeight(2);
        p.ellipse(centerX, centerY, size, size);
    }

    private int resolveTradePropertyStripeColor(fi.monopoly.components.properties.Property property) {
        StreetType streetType = property.getSpotType().streetType;
        if (streetType.color != null) {
            return MonopolyUtils.toColor(runtime.app(), streetType.color);
        }
        if (streetType.placeType == PlaceType.RAILROAD) {
            return runtime.app().color(50, 50, 50);
        }
        if (streetType.placeType == PlaceType.UTILITY) {
            return runtime.app().color(120, 160, 170);
        }
        return runtime.app().color(170, 150, 100);
    }

    private void drawCardFrame(PGraphics p, float x, float y, float width, float height, boolean highlighted, boolean selected) {
        p.rectMode(CORNER);
        if (highlighted) {
            p.noStroke();
            p.fill(runtime.app().color(255, 214, 138, 130));
            p.rect(x - 3f, y - 3f, width + 6f, height + 6f, CARD_RADIUS + 2f);
        }
        p.stroke(highlighted || selected ? p.color(214, 112, 26) : p.color(0));
        p.strokeWeight(selected ? 3.5f : highlighted ? 2.5f : 2f);
        p.fill(selected ? p.color(238, 246, 228) : p.color(245, 250, 241));
        p.rect(x, y, width, height, CARD_RADIUS);

        p.noStroke();
        p.fill(runtime.app().color(CARD_BASE_R, CARD_BASE_G, CARD_BASE_B));
        p.rect(
                x + CARD_INSET,
                y + CARD_INSET,
                width - CARD_INSET * 2f,
                height - CARD_INSET * 2f,
                CARD_INNER_RADIUS
        );
    }

    private void drawCardStripe(PGraphics p, float x, float y, float width, int stripeColor) {
        p.noStroke();
        p.fill(stripeColor);
        p.rect(
                x + CARD_INSET + 1f,
                y + CARD_INSET + 1f,
                width - (CARD_INSET + 1f) * 2f,
                PROPERTY_COLOR_STRIPE_HEIGHT,
                10,
                10,
                4,
                4
        );
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
        if (buttonProps.name().equals(text("trade.button.done"))) {
            button.setButtonColors(
                    runtime.app().color(56, 176, 72),
                    runtime.app().color(76, 204, 90),
                    runtime.app().color(35, 132, 52)
            );
        } else if (buttonProps.name().equals(text("trade.button.clear"))) {
            button.setButtonColors(
                    runtime.app().color(196, 70, 70),
                    runtime.app().color(222, 98, 98),
                    runtime.app().color(150, 44, 44)
            );
        } else {
            button.setButtonColors(
                    runtime.app().color(180, 180, 180),
                    runtime.app().color(220, 220, 220),
                    runtime.app().color(140, 140, 140)
            );
        }
    }

    private void layoutButtons() {
        layoutCloseButton();
        if (totalButtonCount == 0) {
            return;
        }
        if (tradeView != null && tradeView.inventoryTitle() != null && totalButtonCount >= 7) {
            layoutTradeEditorButtons();
            return;
        }
        int cols = Math.min(getMaxButtonColumns(), totalButtonCount);
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

    private void layoutTradeEditorButtons() {
        int top = Math.round(getButtonAreaTop());
        int rowY = top + Math.max(0, Math.round((getButtonAreaHeight() - BUTTON_HEIGHT) / 2f));
        customButtons.get(0).hide();
        List<MonopolyButton> positiveButtons = new ArrayList<>();
        List<MonopolyButton> centerButtons = new ArrayList<>();
        List<MonopolyButton> negativeButtons = new ArrayList<>();
        for (int i = 1; i < totalButtonCount; i++) {
            MonopolyButton button = customButtons.get(i);
            String label = button.getCaptionLabel().getText();
            if (label.startsWith("+")) {
                positiveButtons.add(button);
            } else if (label.startsWith("-")) {
                negativeButtons.add(button);
            } else {
                centerButtons.add(button);
            }
        }
        layoutButtonGroup(positiveButtons, Math.round(getPopupLeft() + 36), rowY, false);
        layoutButtonGroup(centerButtons, Math.round(getPopupCenter().x()), rowY, true);
        layoutButtonGroup(negativeButtons, Math.round(getPopupRight() - 36), rowY, false, true);
        backButton.setPosition(getPopupLeft() + 24, getPopupTop() + 18);
    }

    private void layoutButtonGroup(List<MonopolyButton> buttons, int anchorX, int rowY, boolean centered) {
        layoutButtonGroup(buttons, anchorX, rowY, centered, false);
    }

    private void layoutButtonGroup(List<MonopolyButton> buttons, int anchorX, int rowY, boolean centered, boolean alignRight) {
        if (buttons.isEmpty()) {
            return;
        }
        int totalWidth = buttons.stream().mapToInt(button -> Math.round(button.getWidth())).sum()
                + Math.max(0, buttons.size() - 1) * BUTTON_GAP_X;
        int buttonX = centered ? anchorX - totalWidth / 2 : alignRight ? anchorX - totalWidth : anchorX;
        for (MonopolyButton button : buttons) {
            button.setPosition(buttonX, rowY);
            buttonX += Math.round(button.getWidth()) + BUTTON_GAP_X;
        }
    }

    @Override
    protected void show() {
        super.show();
        layoutButtons();
        closeButton.show();
        if (tradeView != null && tradeView.inventoryTitle() != null && totalButtonCount > 0) {
            backButton.show();
            customButtons.get(0).hide();
        }
        for (int i = 0; i < totalButtonCount; i++) {
            if (tradeView != null && tradeView.inventoryTitle() != null && i == 0) {
                continue;
            }
            customButtons.get(i).show();
        }
    }

    @Override
    protected void hide() {
        super.hide();
        closeButton.hide();
        backButton.hide();
        customButtons.forEach(MonopolyButton::hide);
        activeButtonLabels.clear();
        clickableRegions.clear();
        hoveredItemKey = null;
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
    protected boolean onMouseAction(MouseEvent event) {
        for (ClickableRegion clickableRegion : clickableRegions) {
            if (clickableRegion.contains(event.getX(), event.getY())) {
                if (event.getAction() == CLICK) {
                    completeAction(clickableRegion.action());
                    return true;
                }
                return true;
            }
        }
        return event.getAction() == MOVE;
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
        return getPopupWidth() < 720 ? 3 : 4;
    }

    private float requiredButtonAreaHeight() {
        if (totalButtonCount <= 0) {
            return BUTTON_HEIGHT + BUTTON_AREA_TOP_MARGIN;
        }
        int cols = Math.min(getMaxButtonColumns(), totalButtonCount);
        int rows = (int) Math.ceil((double) totalButtonCount / cols);
        return rows * BUTTON_HEIGHT
                + Math.max(0, rows - 1) * BUTTON_GAP_Y
                + BUTTON_AREA_TOP_MARGIN;
    }

    private void layoutCloseButton() {
        closeButton.setPosition(getPopupRight() - 30, getPopupTop() + 10);
    }

    private boolean isHovered(TradePopupItem item) {
        Object key = hoverKey(item);
        return key != null && key.equals(hoveredItemKey);
    }

    private Object resolveHoveredItemKey(float mouseX, float mouseY) {
        for (ClickableRegion clickableRegion : clickableRegions) {
            if (clickableRegion.contains(mouseX, mouseY)) {
                return clickableRegion.hoverKey();
            }
        }
        return null;
    }

    private Object hoverKey(TradePopupItem item) {
        if (item.property() != null) {
            return item.property();
        }
        if (item.player() != null) {
            return item.player();
        }
        return item.label();
    }

    private record ClickableRegion(float x, float y, float width, float height, ButtonAction action, Object hoverKey) {
        private boolean contains(float px, float py) {
            return px >= x && px <= x + width && py >= y && py <= y + height;
        }
    }
}
