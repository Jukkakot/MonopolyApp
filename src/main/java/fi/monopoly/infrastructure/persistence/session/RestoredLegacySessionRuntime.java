package fi.monopoly.infrastructure.persistence.session;

import fi.monopoly.components.Players;
import fi.monopoly.components.board.Board;

public record RestoredLegacySessionRuntime(
        Board board,
        Players players
) {
}
