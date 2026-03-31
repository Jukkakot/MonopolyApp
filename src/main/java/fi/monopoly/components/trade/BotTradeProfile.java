package fi.monopoly.components.trade;

public enum BotTradeProfile {
    CAUTIOUS(60, 200, -120),
    BALANCED(20, 160, -140),
    AGGRESSIVE(-20, 120, -180);

    private final int acceptThreshold;
    private final int maxCounterAdjustment;
    private final int counterOfferFloor;

    BotTradeProfile(int acceptThreshold, int maxCounterAdjustment, int counterOfferFloor) {
        this.acceptThreshold = acceptThreshold;
        this.maxCounterAdjustment = maxCounterAdjustment;
        this.counterOfferFloor = counterOfferFloor;
    }

    public int acceptThreshold() {
        return acceptThreshold;
    }

    public int maxCounterAdjustment() {
        return maxCounterAdjustment;
    }

    public int counterOfferFloor() {
        return counterOfferFloor;
    }
}
