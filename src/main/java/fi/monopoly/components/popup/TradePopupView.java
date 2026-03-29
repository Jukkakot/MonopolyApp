package fi.monopoly.components.popup;

import fi.monopoly.components.Player;

import java.util.List;

public record TradePopupView(
        String title,
        String subtitle,
        Player leftPlayer,
        List<TradePopupItem> leftItems,
        boolean highlightLeft,
        Player rightPlayer,
        List<TradePopupItem> rightItems,
        boolean highlightRight,
        String footer,
        String inventoryTitle,
        List<TradePopupItem> inventoryItems
) {
}
