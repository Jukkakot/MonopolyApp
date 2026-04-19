package fi.monopoly.infrastructure.persistence.session;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.board.Board;

import java.util.Map;

public record RestoredLegacySessionRuntime(
        Board board,
        Players players,
        Map<String, Player> playersById
) {
}
