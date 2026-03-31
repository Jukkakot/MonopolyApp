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
import fi.monopoly.utils.LayoutMetrics;
import fi.monopoly.utils.MonopolyUtils;
import fi.monopoly.utils.UiTokens;
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
    private static final float INVENTORY_CARD_W = Spot.SPOT_W;
    private static final float INVENTORY_CARD_H = Spot.SPOT_H;
    private static final float SUMMARY_CARD_W = Spot.SPOT_W;
    private static final float SUMMARY_CARD_H = Spot.SPOT_H;
    private static final int CARD_BASE_R = 205;
    private static final int CARD_BASE_G = 230;
    private static final int CARD_BASE_B = 209;

    private final List<MonopolyButton> customButtons = new ArrayList<>();
    private final List<String> activeButtonLabels = new ArrayList<>();
    private final List<ButtonAction> activeButtonActions = new ArrayList<>();
    private final List<ClickableRegion> clickableRegions = new ArrayList<>();
    private final MonopolyButton closeButton;
    private final MonopolyButton backButton;
    private int totalButtonCount = 0;
    private TradePopupView tradeView;
    private Object hoveredItemKey;

    protected TradePopup(MonopolyRuntime runtime) {
        super(runtime);
        this.closeButton = new MonopolyButton(runtime, "tradeClose")
                .addListener(() -> completeManualAction(null))
                .setSize(UiTokens.popupCloseButtonSize(), UiTokens.popupCloseButtonSize());
        this.backButton = new MonopolyButton(runtime, "tradeBack")
                .addListener(() -> {
                    if (tradeView != null && tradeView.backAction() != null) {
                        completeManualAction(tradeView.backAction());
                    }
                })
                .setSize(UiTokens.tradeBackButtonWidth(), UiTokens.tradeBackButtonHeight());
        closeButton.hide();
        backButton.hide();
        refreshLabels();
        fi.monopoly.text.UiTexts.addChangeListener(this::refreshLabels);
    }

    private void refreshLabels() {
        closeButton.setLabel(text("popup.close.label"));
        backButton.setLabel("<");
    }

    public void setTradeView(TradePopupView tradeView) {
        this.tradeView = tradeView;
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

    @Override
    protected int getPopupWidth() {
        int availableWidth = Math.round(runtime.app().width) - UiTokens.popupWindowMargin() * 2;
        return Math.max(UiTokens.tradePopupMinWidth(), Math.min(UiTokens.tradePopupPreferredWidth(), availableWidth));
    }

    @Override
    protected int getPopupHeight() {
        int availableHeight = runtime.app().height - UiTokens.popupWindowMargin() * 2;
        return Math.max(UiTokens.tradePopupMinHeight(), Math.min(UiTokens.tradePopupPreferredHeight(), availableHeight));
    }

    @Override
    protected fi.monopoly.utils.Coordinates getPopupCenter() {
        return fi.monopoly.utils.Coordinates.of(runtime.app().width / 2f, getPopupTop() + getPopupHeight() / 2f);
    }

    @Override
    protected float getPopupTop() {
        return UiTokens.tradePopupTopMargin();
    }

    @Override
    protected float getButtonAreaTop() {
        return getPopupBottom() - UiTokens.tradeButtonAreaBottomMargin() - requiredButtonAreaHeight();
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
        float panelWidth = (width - UiTokens.popupTextSidePadding() * 2f - UiTokens.tradePanelGap()) / 2f;
        float leftPanelX = left + UiTokens.popupTextSidePadding();
        float rightPanelX = leftPanelX + panelWidth + UiTokens.tradePanelGap();
        float panelY = top + UiTokens.tradePanelsTopOffset();

        p.pushMatrix();
        p.pushStyle();
        p.resetMatrix();

        p.fill(0);
        p.textFont(runtime.font30());
        p.textAlign(CENTER, TOP);
        p.text(tradeView.title(), getPopupCenter().x(), top + UiTokens.popupTextTopOffset());
        if (tradeView.subtitle() != null && !tradeView.subtitle().isBlank()) {
            p.textFont(runtime.font20());
            p.text(tradeView.subtitle(), getPopupCenter().x(), top + UiTokens.tradeSubtitleTopOffset() + 4f);
        }

        drawPanel(p, leftPanelX, panelY, panelWidth, UiTokens.tradePanelsHeight(), tradeView.leftPlayer(), tradeView.leftItems(), tradeView.highlightLeft());
        drawPanel(p, rightPanelX, panelY, panelWidth, UiTokens.tradePanelsHeight(), tradeView.rightPlayer(), tradeView.rightItems(), tradeView.highlightRight());
        if (tradeView.leftAction() != null) {
            clickableRegions.add(new ClickableRegion(leftPanelX, panelY, panelWidth, UiTokens.tradePanelsHeight(), tradeView.leftAction(), tradeView.leftPlayer()));
        }
        if (tradeView.rightAction() != null) {
            clickableRegions.add(new ClickableRegion(rightPanelX, panelY, panelWidth, UiTokens.tradePanelsHeight(), tradeView.rightAction(), tradeView.rightPlayer()));
        }

        float inventoryTitleY = panelY + UiTokens.tradePanelsHeight() + UiTokens.tradeInventoryTopGap();
        if (tradeView.inventoryTitle() != null && !tradeView.inventoryTitle().isBlank()) {
            p.fill(0);
            p.textFont(runtime.font20());
            p.textAlign(LEFT, TOP);
            p.text(tradeView.inventoryTitle(), left + UiTokens.popupTextSidePadding(), inventoryTitleY);
        }
        drawInventory(
                p,
                left + UiTokens.popupTextSidePadding(),
                inventoryTitleY + UiTokens.tradeInventoryTitleGap(),
                width - UiTokens.popupTextSidePadding() * 2f,
                getButtonAreaTop() - (inventoryTitleY + UiTokens.tradeInventoryTitleGap()) - UiTokens.spacingSm()
        );

        if (tradeView.footer() != null && !tradeView.footer().isBlank()) {
            p.fill(p.color(85, 42, 0));
            p.textFont(runtime.font20());
            p.textAlign(CENTER, TOP);
            p.text(tradeView.footer(), getPopupCenter().x(), getButtonAreaTop() - UiTokens.tradeFooterBottomPadding() - 6f);
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

        float itemX = x + UiTokens.tradePanelPadding();
        float itemY = y + UiTokens.tradePanelPadding() + UiTokens.tradePanelHeaderHeight();
        float maxX = x + width - UiTokens.tradePanelPadding();
        for (TradePopupItem item : items) {
            float cardW = (item.type() == TradePopupItemType.PROPERTY || item.type() == TradePopupItemType.JAIL_CARD) ? SUMMARY_CARD_W : 112f;
            float cardH = (item.type() == TradePopupItemType.PROPERTY || item.type() == TradePopupItemType.JAIL_CARD) ? SUMMARY_CARD_H : 48f;
            if (itemX + cardW > maxX) {
                itemX = x + UiTokens.tradePanelPadding();
                itemY += cardH + UiTokens.tradeItemGap();
            }
            boolean itemHighlighted = item.selected() || isHovered(item) || highlighted;
            drawTradeItem(p, item, itemX, itemY, cardW, cardH, itemHighlighted, false);
            if (item.action() != null) {
                clickableRegions.add(new ClickableRegion(itemX, itemY, cardW, cardH, item.action(), hoverKey(item)));
            }
            itemX += cardW + UiTokens.tradeItemGap();
        }
    }

    private void drawPlayerHeader(PGraphics p, float x, float y, float width, Player player, boolean highlighted) {
        if (player == null) {
            return;
        }
        float tokenX = x + UiTokens.tradePanelPadding() + UiTokens.tradeTokenSize() / 2f;
        float tokenY = y + UiTokens.tradePanelPadding() + UiTokens.tradeTokenSize() / 2f + 2f;
        drawPlayerToken(p, player, tokenX, tokenY, UiTokens.tradeTokenSize());
        p.fill(highlighted ? p.color(110, 60, 12) : p.color(0));
        p.textFont(runtime.font20());
        p.textAlign(LEFT, TOP);
        p.text(player.getName(), x + UiTokens.tradePanelPadding() + UiTokens.tradeTokenSize() + 10f, y + UiTokens.tradePanelPadding());
    }

    private void drawInventory(PGraphics p, float startX, float startY, float availableWidth, float availableHeight) {
        List<TradePopupItem> items = tradeView.inventoryItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        int columns = Math.max(1, Math.min(items.size(), (int) Math.floor((availableWidth + UiTokens.tradeItemGap()) / (INVENTORY_CARD_W + UiTokens.tradeItemGap()))));
        float x = startX;
        float y = startY;
        int indexInRow = 0;
        for (TradePopupItem item : items) {
            if (indexInRow >= columns) {
                indexInRow = 0;
                x = startX;
                y += INVENTORY_CARD_H + UiTokens.tradeItemGap();
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
            x += INVENTORY_CARD_W + UiTokens.tradeItemGap();
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
            p.image(image, x + 8f, y + 8f + UiTokens.tradePropertyColorStripeHeight(), width - 16f, height - (showLabel ? 42f : 16f) - UiTokens.tradePropertyColorStripeHeight());
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
            p.rect(x - 3f, y - 3f, width + 6f, height + 6f, UiTokens.tradeCardRadius() + 2f);
        }
        p.stroke(highlighted || selected ? p.color(214, 112, 26) : p.color(0));
        p.strokeWeight(selected ? 3.5f : highlighted ? 2.5f : 2f);
        p.fill(selected ? p.color(238, 246, 228) : p.color(245, 250, 241));
        p.rect(x, y, width, height, UiTokens.tradeCardRadius());

        p.noStroke();
        p.fill(runtime.app().color(CARD_BASE_R, CARD_BASE_G, CARD_BASE_B));
        p.rect(
                x + UiTokens.tradeCardInset(),
                y + UiTokens.tradeCardInset(),
                width - UiTokens.tradeCardInset() * 2f,
                height - UiTokens.tradeCardInset() * 2f,
                UiTokens.tradeCardInnerRadius()
        );
    }

    private void drawCardStripe(PGraphics p, float x, float y, float width, int stripeColor) {
        p.noStroke();
        p.fill(stripeColor);
        p.rect(
                x + UiTokens.tradeCardInset() + 1f,
                y + UiTokens.tradeCardInset() + 1f,
                width - (UiTokens.tradeCardInset() + 1f) * 2f,
                UiTokens.tradePropertyColorStripeHeight(),
                10,
                10,
                4,
                4
        );
    }

    private void ensureButtonPoolSize(int requiredCount) {
        while (customButtons.size() < requiredCount) {
            MonopolyButton button = new MonopolyButton(runtime, "tradeCustomButton" + customButtons.size());
            button.setSize(UiTokens.popupButtonMinWidth(), UiTokens.popupButtonHeight());
            button.setAutoWidth(UiTokens.popupButtonMinWidth(), UiTokens.popupButtonPadding(), UiTokens.popupButtonMaxWidth());
            button.hide();
            customButtons.add(button);
        }
    }

    private void configureButton(MonopolyButton button, ButtonProps buttonProps) {
        button.setLabel(buttonProps.name());
        button.addListener(() -> completeManualAction(buttonProps.buttonAction()));
        button.setSize(UiTokens.popupButtonMinWidth(), UiTokens.popupButtonHeight());
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
        layoutBackButton();
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
        int totalHeight = rows * UiTokens.popupButtonHeight() + Math.max(0, rows - 1) * UiTokens.popupButtonGapY();
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
            int rowY = startY + row * (UiTokens.popupButtonHeight() + UiTokens.popupButtonGapY());
            int buttonX = startX;
            for (int i = rowStart; i < rowEnd; i++) {
                MonopolyButton button = customButtons.get(i);
                button.setPosition(buttonX, rowY);
                buttonX += Math.round(button.getWidth()) + UiTokens.popupButtonGapX();
            }
        }
    }

    private void layoutTradeEditorButtons() {
        int top = Math.round(getButtonAreaTop());
        int rowY = top + Math.max(0, Math.round((getButtonAreaHeight() - UiTokens.popupButtonHeight()) / 2f));
        List<MonopolyButton> positiveButtons = new ArrayList<>();
        List<MonopolyButton> centerButtons = new ArrayList<>();
        List<MonopolyButton> negativeButtons = new ArrayList<>();
        for (int i = 0; i < totalButtonCount; i++) {
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
        layoutButtonGroup(negativeButtons, Math.round(getPopupLeft() + 36), rowY, false);
        layoutButtonGroup(centerButtons, Math.round(getPopupCenter().x()), rowY, true);
        layoutButtonGroup(positiveButtons, Math.round(getPopupRight() - 36), rowY, false, true);
    }

    private void layoutBackButton() {
        backButton.setPosition(
                getPopupLeft() + UiTokens.tradeBackButtonLeftInset(),
                getPopupTop() + UiTokens.tradeBackButtonTopInset()
        );
    }

    private void layoutButtonGroup(List<MonopolyButton> buttons, int anchorX, int rowY, boolean centered) {
        layoutButtonGroup(buttons, anchorX, rowY, centered, false);
    }

    private void layoutButtonGroup(List<MonopolyButton> buttons, int anchorX, int rowY, boolean centered, boolean alignRight) {
        if (buttons.isEmpty()) {
            return;
        }
        int totalWidth = buttons.stream().mapToInt(button -> Math.round(button.getWidth())).sum()
                + Math.max(0, buttons.size() - 1) * UiTokens.popupButtonGapX();
        int buttonX = centered ? anchorX - totalWidth / 2 : alignRight ? anchorX - totalWidth : anchorX;
        for (MonopolyButton button : buttons) {
            button.setPosition(buttonX, rowY);
            buttonX += Math.round(button.getWidth()) + UiTokens.popupButtonGapX();
        }
    }

    @Override
    protected void show() {
        super.show();
        layoutButtons();
        closeButton.show();
        if (tradeView != null && tradeView.backAction() != null) {
            backButton.show();
        }
        for (int i = 0; i < totalButtonCount; i++) {
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
        activeButtonActions.clear();
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
            completeManualAction(null);
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
                    completeManualAction(clickableRegion.action());
                    return true;
                }
                return true;
            }
        }
        return event.getAction() == MOVE;
    }

    @Override
    protected boolean onComputerAction(ComputerPlayerProfile profile) {
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

    private ButtonAction getButtonActionAt(int index) {
        if (index < 0 || index >= totalButtonCount) {
            return null;
        }
        return activeButtonActions.get(index);
    }

    private int getMaxButtonColumns() {
        return getPopupWidth() < 720 ? 3 : 4;
    }

    private float requiredButtonAreaHeight() {
        if (totalButtonCount <= 0) {
            return UiTokens.popupButtonHeight() + UiTokens.tradeButtonAreaTopMargin();
        }
        int cols = Math.min(getMaxButtonColumns(), totalButtonCount);
        int rows = (int) Math.ceil((double) totalButtonCount / cols);
        return rows * UiTokens.popupButtonHeight()
                + Math.max(0, rows - 1) * UiTokens.popupButtonGapY()
                + UiTokens.tradeButtonAreaTopMargin();
    }

    private void layoutCloseButton() {
        closeButton.setPosition(
                getPopupRight() - UiTokens.popupCloseButtonRightInset(),
                getPopupTop() + UiTokens.popupCloseButtonTopInset()
        );
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
