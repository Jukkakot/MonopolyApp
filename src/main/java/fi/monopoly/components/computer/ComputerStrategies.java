package fi.monopoly.components.computer;

public final class ComputerStrategies {
    private static final ComputerTurnStrategy SMOKE_TEST_STRATEGY = new SmokeTestComputerStrategy();

    private ComputerStrategies() {
    }

    public static ComputerTurnStrategy forProfile(ComputerPlayerProfile profile) {
        return switch (profile) {
            case HUMAN -> throw new IllegalArgumentException("Human players do not have a computer strategy");
            case SMOKE_TEST -> SMOKE_TEST_STRATEGY;
        };
    }
}
