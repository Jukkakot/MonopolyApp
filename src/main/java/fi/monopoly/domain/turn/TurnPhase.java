package fi.monopoly.domain.turn;

public enum TurnPhase {
    WAITING_FOR_ROLL,
    WAITING_FOR_DECISION,
    RESOLVING_DEBT,
    WAITING_FOR_END_TURN,
    GAME_OVER,
    UNKNOWN
}
