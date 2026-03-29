package fi.monopoly.components.popup;

import fi.monopoly.components.Player;
import fi.monopoly.components.popup.ButtonAction;

import java.util.List;

public record TradePopupView(
        String title,
        String subtitle,
        Player leftPlayer,
        List<TradePopupItem> leftItems,
        boolean highlightLeft,
        ButtonAction leftAction,
        Player rightPlayer,
        List<TradePopupItem> rightItems,
        boolean highlightRight,
        ButtonAction rightAction,
        String footer,
        String inventoryTitle,
        List<TradePopupItem> inventoryItems
) {
}
