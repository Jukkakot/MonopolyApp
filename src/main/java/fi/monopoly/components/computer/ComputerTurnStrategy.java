package fi.monopoly.components.computer;

public interface ComputerTurnStrategy {
    boolean takeStep(ComputerTurnContext context);
}
