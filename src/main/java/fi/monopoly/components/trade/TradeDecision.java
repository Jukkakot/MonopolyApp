package fi.monopoly.components.trade;

public record TradeDecision(
        boolean accept,
        double score,
        String reason
) {
}
