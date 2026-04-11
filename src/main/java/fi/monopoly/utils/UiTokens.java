package fi.monopoly.utils;

import fi.monopoly.MonopolyApp;

/**
 * Centralized static UI design tokens shared across screens and components.
 * Keep window-derived runtime calculations in {@link LayoutMetrics}.
 */
public final class UiTokens {
    private static final int MIN_FIXED_LAYOUT_WINDOW_WIDTH = MonopolyApp.DEFAULT_WINDOW_WIDTH;
    private static final int MIN_FIXED_LAYOUT_WINDOW_HEIGHT = MonopolyApp.DEFAULT_WINDOW_HEIGHT;
    private static final int SPACING_XS = 8;
    private static final int SPACING_SM = 12;
    private static final int SPACING_MD = 16;
    private static final int SPACING_LG = 20;
    private static final int SPACING_XL = 24;
    private static final int SIDEBAR_LABEL_X = 16;
    private static final int SIDEBAR_VALUE_X = 192;
    private static final int SIDEBAR_MIN_CONTENT_TOP = 220;
    private static final int SIDEBAR_HISTORY_PREFERRED_HEIGHT = 192;
    private static final int SIDEBAR_HISTORY_MIN_HEIGHT = 112;
    private static final int SIDEBAR_HISTORY_HEADER_HEIGHT = 32;
    private static final int SIDEBAR_HISTORY_TEXT_INSET = 8;
    private static final int SIDEBAR_HISTORY_TOP_MARGIN = 16;
    private static final int SIDEBAR_LINE_HEIGHT = 24;
    private static final int OVERLAY_PRIMARY_BUTTON_Y = 16;
    private static final int OVERLAY_SECONDARY_ROW_1_Y = 68;
    private static final int OVERLAY_SECONDARY_ROW_2_Y = 112;
    private static final int OVERLAY_SECONDARY_ROW_3_Y = 156;
    private static final int POPUP_DEFAULT_WIDTH = 500;
    private static final int POPUP_DEFAULT_HEIGHT = 300;
    private static final int POPUP_MIN_WIDTH = 320;
    private static final int POPUP_MIN_HEIGHT = 220;
    private static final int POPUP_WINDOW_MARGIN = 32;
    private static final int POPUP_TEXT_TOP_OFFSET = 24;
    private static final int POPUP_TEXT_SIDE_PADDING = 20;
    private static final int POPUP_TEXT_BOTTOM_PADDING = 16;
    private static final int POPUP_TEXT_LINE_HEIGHT = 24;
    private static final int POPUP_BUTTON_AREA_TOP_PADDING = 90;
    private static final int POPUP_BUTTON_AREA_BOTTOM_PADDING = 30;
    private static final int POPUP_BUTTON_MIN_WIDTH = 100;
    private static final int POPUP_BUTTON_MAX_WIDTH = 220;
    private static final int POPUP_BUTTON_HEIGHT = 50;
    private static final int POPUP_BUTTON_PADDING = 28;
    private static final int POPUP_BUTTON_GAP_X = 12;
    private static final int POPUP_BUTTON_GAP_Y = 12;
    private static final int POPUP_CLOSE_BUTTON_SIZE = 20;
    private static final int POPUP_CLOSE_BUTTON_RIGHT_INSET = 30;
    private static final int POPUP_CLOSE_BUTTON_TOP_INSET = 10;
    private static final int DICE_BUTTON_HEIGHT = 44;
    private static final int DICE_BUTTON_OFFSET_FROM_PRIMARY = -8;
    private static final int DICE_STACKED_VERTICAL_OFFSET = 56;
    private static final int TRADE_POPUP_PREFERRED_WIDTH = 980;
    private static final int TRADE_POPUP_PREFERRED_HEIGHT = 820;
    private static final int TRADE_POPUP_MIN_WIDTH = 560;
    private static final int TRADE_POPUP_MIN_HEIGHT = 420;
    private static final float TRADE_POPUP_TOP_MARGIN = 6f;
    private static final float TRADE_PANELS_TOP_OFFSET = 82f;
    private static final float TRADE_PANELS_HEIGHT = 240f;
    private static final float TRADE_PANEL_GAP = 18f;
    private static final float TRADE_PANEL_PADDING = 14f;
    private static final float TRADE_PANEL_HEADER_HEIGHT = 46f;
    private static final float TRADE_ITEM_GAP = 10f;
    private static final float TRADE_INVENTORY_TOP_GAP = 18f;
    private static final float TRADE_INVENTORY_TITLE_GAP = 28f;
    private static final float TRADE_TOKEN_SIZE = 34f;
    private static final float TRADE_SUBTITLE_TOP_OFFSET = 40f;
    private static final float TRADE_FOOTER_BOTTOM_PADDING = 22f;
    private static final float TRADE_PROPERTY_COLOR_STRIPE_HEIGHT = 18f;
    private static final float TRADE_BUTTON_AREA_BOTTOM_MARGIN = 28f;
    private static final float TRADE_BUTTON_AREA_TOP_MARGIN = 18f;
    private static final int TRADE_MONEY_BUTTON_GAP_X = 20;
    private static final float TRADE_CARD_RADIUS = 14f;
    private static final float TRADE_CARD_INNER_RADIUS = 11f;
    private static final int TRADE_CARD_INSET = 4;
    private static final int TRADE_BACK_BUTTON_WIDTH = 56;
    private static final int TRADE_BACK_BUTTON_HEIGHT = 32;
    private static final int TRADE_BACK_BUTTON_LEFT_INSET = 24;
    private static final int TRADE_BACK_BUTTON_TOP_INSET = 18;

    private UiTokens() {
    }

    public static int minimumFixedLayoutWindowWidth() { return MIN_FIXED_LAYOUT_WINDOW_WIDTH; }
    public static int minimumFixedLayoutWindowHeight() { return MIN_FIXED_LAYOUT_WINDOW_HEIGHT; }
    public static int spacingXs() { return SPACING_XS; }
    public static int spacingSm() { return SPACING_SM; }
    public static int spacingMd() { return SPACING_MD; }
    public static int spacingLg() { return SPACING_LG; }
    public static int spacingXl() { return SPACING_XL; }
    public static int sidebarLabelX() { return SIDEBAR_LABEL_X; }
    public static int sidebarValueX() { return SIDEBAR_VALUE_X; }
    public static int sidebarMinContentTop() { return SIDEBAR_MIN_CONTENT_TOP; }
    public static int sidebarHistoryPreferredHeight() { return SIDEBAR_HISTORY_PREFERRED_HEIGHT; }
    public static int sidebarHistoryMinHeight() { return SIDEBAR_HISTORY_MIN_HEIGHT; }
    public static int sidebarHistoryHeaderHeight() { return SIDEBAR_HISTORY_HEADER_HEIGHT; }
    public static int sidebarHistoryTextInset() { return SIDEBAR_HISTORY_TEXT_INSET; }
    public static int sidebarHistoryTopMargin() { return SIDEBAR_HISTORY_TOP_MARGIN; }
    public static int sidebarLineHeight() { return SIDEBAR_LINE_HEIGHT; }
    public static int overlayMargin() { return SPACING_MD; }
    public static int overlayPrimaryButtonY() { return OVERLAY_PRIMARY_BUTTON_Y; }
    public static int overlaySecondaryRow1Y() { return OVERLAY_SECONDARY_ROW_1_Y; }
    public static int overlaySecondaryRow2Y() { return OVERLAY_SECONDARY_ROW_2_Y; }
    public static int overlaySecondaryRow3Y() { return OVERLAY_SECONDARY_ROW_3_Y; }
    public static int popupDefaultWidth() { return POPUP_DEFAULT_WIDTH; }
    public static int popupDefaultHeight() { return POPUP_DEFAULT_HEIGHT; }
    public static int popupMinWidth() { return POPUP_MIN_WIDTH; }
    public static int popupMinHeight() { return POPUP_MIN_HEIGHT; }
    public static int popupWindowMargin() { return POPUP_WINDOW_MARGIN; }
    public static int popupTextTopOffset() { return POPUP_TEXT_TOP_OFFSET; }
    public static int popupTextSidePadding() { return POPUP_TEXT_SIDE_PADDING; }
    public static int popupTextBottomPadding() { return POPUP_TEXT_BOTTOM_PADDING; }
    public static int popupTextLineHeight() { return POPUP_TEXT_LINE_HEIGHT; }
    public static int popupButtonAreaTopPadding() { return POPUP_BUTTON_AREA_TOP_PADDING; }
    public static int popupButtonAreaBottomPadding() { return POPUP_BUTTON_AREA_BOTTOM_PADDING; }
    public static int popupButtonMinWidth() { return POPUP_BUTTON_MIN_WIDTH; }
    public static int popupButtonMaxWidth() { return POPUP_BUTTON_MAX_WIDTH; }
    public static int popupButtonHeight() { return POPUP_BUTTON_HEIGHT; }
    public static int popupButtonPadding() { return POPUP_BUTTON_PADDING; }
    public static int popupButtonGapX() { return POPUP_BUTTON_GAP_X; }
    public static int popupButtonGapY() { return POPUP_BUTTON_GAP_Y; }
    public static int popupCloseButtonSize() { return POPUP_CLOSE_BUTTON_SIZE; }
    public static int popupCloseButtonRightInset() { return POPUP_CLOSE_BUTTON_RIGHT_INSET; }
    public static int popupCloseButtonTopInset() { return POPUP_CLOSE_BUTTON_TOP_INSET; }
    public static int diceButtonHeight() { return DICE_BUTTON_HEIGHT; }
    public static int diceButtonOffsetFromPrimary() { return DICE_BUTTON_OFFSET_FROM_PRIMARY; }
    public static int diceStackedVerticalOffset() { return DICE_STACKED_VERTICAL_OFFSET; }
    public static int tradePopupPreferredWidth() { return TRADE_POPUP_PREFERRED_WIDTH; }
    public static int tradePopupPreferredHeight() { return TRADE_POPUP_PREFERRED_HEIGHT; }
    public static int tradePopupMinWidth() { return TRADE_POPUP_MIN_WIDTH; }
    public static int tradePopupMinHeight() { return TRADE_POPUP_MIN_HEIGHT; }
    public static float tradePopupTopMargin() { return TRADE_POPUP_TOP_MARGIN; }
    public static float tradePanelsTopOffset() { return TRADE_PANELS_TOP_OFFSET; }
    public static float tradePanelsHeight() { return TRADE_PANELS_HEIGHT; }
    public static float tradePanelGap() { return TRADE_PANEL_GAP; }
    public static float tradePanelPadding() { return TRADE_PANEL_PADDING; }
    public static float tradePanelHeaderHeight() { return TRADE_PANEL_HEADER_HEIGHT; }
    public static float tradeItemGap() { return TRADE_ITEM_GAP; }
    public static float tradeInventoryTopGap() { return TRADE_INVENTORY_TOP_GAP; }
    public static float tradeInventoryTitleGap() { return TRADE_INVENTORY_TITLE_GAP; }
    public static float tradeTokenSize() { return TRADE_TOKEN_SIZE; }
    public static float tradeSubtitleTopOffset() { return TRADE_SUBTITLE_TOP_OFFSET; }
    public static float tradeFooterBottomPadding() { return TRADE_FOOTER_BOTTOM_PADDING; }
    public static float tradePropertyColorStripeHeight() { return TRADE_PROPERTY_COLOR_STRIPE_HEIGHT; }
    public static float tradeButtonAreaBottomMargin() { return TRADE_BUTTON_AREA_BOTTOM_MARGIN; }
    public static float tradeButtonAreaTopMargin() { return TRADE_BUTTON_AREA_TOP_MARGIN; }
    public static int tradeMoneyButtonGapX() { return TRADE_MONEY_BUTTON_GAP_X; }
    public static float tradeCardRadius() { return TRADE_CARD_RADIUS; }
    public static float tradeCardInnerRadius() { return TRADE_CARD_INNER_RADIUS; }
    public static int tradeCardInset() { return TRADE_CARD_INSET; }
    public static int tradeBackButtonWidth() { return TRADE_BACK_BUTTON_WIDTH; }
    public static int tradeBackButtonHeight() { return TRADE_BACK_BUTTON_HEIGHT; }
    public static int tradeBackButtonLeftInset() { return TRADE_BACK_BUTTON_LEFT_INSET; }
    public static int tradeBackButtonTopInset() { return TRADE_BACK_BUTTON_TOP_INSET; }
}
