package fi.monopoly.components.computer;

public record ComputerDecision(
        ComputerAction action,
        double score,
        String reason
) {
}
