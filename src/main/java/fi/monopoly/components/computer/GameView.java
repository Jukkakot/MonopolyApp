package fi.monopoly.components.computer;

import java.util.List;
import java.util.Optional;

public record GameView(
        int currentPlayerId,
        List<PlayerView> players,
        VisibleActionsView visibleActions,
        PopupView popup,
        DebtView debt,
        int unownedPropertyCount,
        int bankHousesLeft,
        int bankHotelsLeft
) {
    public GameView {
        players = List.copyOf(players);
    }

    public Optional<PlayerView> currentPlayer() {
        return players.stream()
                .filter(player -> player.id() == currentPlayerId)
                .findFirst();
    }
}
