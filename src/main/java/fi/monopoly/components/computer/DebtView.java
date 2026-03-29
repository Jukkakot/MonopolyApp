package fi.monopoly.components.computer;

public record DebtView(
        int amount,
        String reason,
        boolean bankruptcyRisk,
        String targetType,
        String targetName
) {
}
