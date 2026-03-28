package fi.monopoly.components.computer;

public enum ComputerPlayerProfile {
    HUMAN,
    SMOKE_TEST;

    public boolean isComputerControlled() {
        return this != HUMAN;
    }
}
