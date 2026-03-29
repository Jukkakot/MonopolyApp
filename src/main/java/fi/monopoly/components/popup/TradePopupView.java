package fi.monopoly.components.popup;

import java.util.List;

public record TradePopupView(
        String title,
        String subtitle,
        String leftPlayerName,
        List<String> leftItems,
        boolean highlightLeft,
        String rightPlayerName,
        List<String> rightItems,
        boolean highlightRight,
        String footer
) {
}
