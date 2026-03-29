package fi.monopoly.components.computer;

public enum ComputerPlayerProfile {
    HUMAN,
    SMOKE_TEST,
    STRONG;

    public boolean isComputerControlled() {
        return this != HUMAN;
    }
}
