package fi.monopoly.components.computer;

public record VisibleActionsView(
        boolean popupVisible,
        boolean retryDebtVisible,
        boolean declareBankruptcyVisible,
        boolean rollDiceVisible,
        boolean endTurnVisible
) {
}
