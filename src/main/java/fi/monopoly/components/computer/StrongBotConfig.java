package fi.monopoly.components.computer;

public record StrongBotConfig(
        double buyThreshold,
        int minCashReserve,
        int dangerCashReserve,
        double completionWeight,
        double progressWeight,
        double opponentBlockWeight,
        double railroadWeight,
        double utilityWeight,
        double liquidityPenaltyWeight,
        boolean buyToBlockOpponent
) {
    public static StrongBotConfig defaults() {
        return new StrongBotConfig(
                6.5,
                250,
                400,
                9.0,
                3.0,
                6.0,
                2.5,
                0.5,
                3.0,
                true
        );
    }
}
