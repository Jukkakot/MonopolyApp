package fi.monopoly.domain.session;

import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.turn.TurnState;

import java.util.List;

public record SessionState(
        String sessionId,
        long version,
        SessionStatus status,
        List<SeatState> seats,
        List<PlayerSnapshot> players,
        List<PropertyStateSnapshot> properties,
        TurnState turn,
        PendingDecision pendingDecision,
        AuctionState auctionState,
        DebtStateModel activeDebt,
        TradeState tradeState,
        String winnerPlayerId
) {
    public SessionState {
        seats = List.copyOf(seats);
        players = List.copyOf(players);
        properties = List.copyOf(properties);
    }
}
