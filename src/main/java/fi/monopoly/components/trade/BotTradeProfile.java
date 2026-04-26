package fi.monopoly.components.trade;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum BotTradeProfile {
    CAUTIOUS(60, 200, -120),
    BALANCED(20, 160, -140),
    AGGRESSIVE(-20, 120, -180);

    private final int acceptThreshold;
    private final int maxCounterAdjustment;
    private final int counterOfferFloor;
}
