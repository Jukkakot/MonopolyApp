package fi.monopoly.components.trade;

public enum BotTradeProfile {
    CAUTIOUS(60, 200),
    BALANCED(20, 160),
    AGGRESSIVE(-20, 120);

    private final int acceptThreshold;
    private final int maxCounterAdjustment;

    BotTradeProfile(int acceptThreshold, int maxCounterAdjustment) {
        this.acceptThreshold = acceptThreshold;
        this.maxCounterAdjustment = maxCounterAdjustment;
    }

    public int acceptThreshold() {
        return acceptThreshold;
    }

    public int maxCounterAdjustment() {
        return maxCounterAdjustment;
    }
}
