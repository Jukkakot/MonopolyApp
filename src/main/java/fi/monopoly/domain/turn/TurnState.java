package fi.monopoly.domain.turn;

public record TurnState(
        String activePlayerId,
        TurnPhase phase,
        boolean canRoll,
        boolean canEndTurn
) {
}
