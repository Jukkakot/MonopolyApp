package fi.monopoly.server.session;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that {@link PureDomainSessionFactory} produces a working
 * {@link SessionApplicationService} with no Processing runtime dependency.
 */
class PureDomainSessionFactoryTest {

    private static final String SESSION_ID = "pure-session";
    private static final String PLAYER_1 = "player-1";
    private static final String PROPERTY_B1 = "B1";

    @Test
    void emptyInitialStateHasCorrectSessionId() {
        SessionState state = PureDomainSessionFactory.emptyInitialState(SESSION_ID);
        assertEquals(SESSION_ID, state.sessionId());
        assertEquals(SessionStatus.IN_PROGRESS, state.status());
    }

    @Test
    void declinePropertyCommandRejectedWithNoActivePurchase() {
        SessionApplicationService service = factoryWithPlayer(PLAYER_1, 1500);

        CommandResult result = service.handle(new DeclinePropertyCommand(SESSION_ID, PLAYER_1, "decision-1", PROPERTY_B1));

        assertFalse(result.accepted());
    }

    @Test
    void auctionCommandRejectedWhenNoAuctionActive() {
        SessionApplicationService service = factoryWithPlayer(PLAYER_1, 1500);

        CommandResult result = service.handle(
                new PlaceAuctionBidCommand(SESSION_ID, PLAYER_1, "auction-1", 10));

        assertFalse(result.accepted());
    }

    @Test
    void buyPropertyCommandRejectedWithNoActivePurchase() {
        SessionApplicationService service = factoryWithPlayer(PLAYER_1, 1500);

        CommandResult result = service.handle(new BuyPropertyCommand(SESSION_ID, PLAYER_1, "decision-1", PROPERTY_B1));

        assertFalse(result.accepted());
    }

    @Test
    void currentStateReflectsInitialPlayers() {
        SessionApplicationService service = factoryWithPlayer(PLAYER_1, 1500);

        SessionState state = service.currentState();

        assertEquals(1, state.players().size());
        assertEquals(PLAYER_1, state.players().get(0).playerId());
        assertEquals(1500, state.players().get(0).cash());
    }

    private static SessionApplicationService factoryWithPlayer(String playerId, int cash) {
        SessionState initialState = buildState(
                List.of(player(playerId, 0, cash)),
                List.of()
        );
        return PureDomainSessionFactory.create(SESSION_ID, initialState);
    }

    private static SessionState buildState(List<PlayerSnapshot> players, List<PropertyStateSnapshot> properties) {
        List<SeatState> seats = players.stream()
                .map(p -> {
                    int idx = players.indexOf(p);
                    return new SeatState("seat-" + idx, idx, p.playerId(),
                            SeatKind.HUMAN, ControlMode.MANUAL, p.name(), "HUMAN", "#000000");
                })
                .toList();
        return new SessionState(
                SESSION_ID, 0L, SessionStatus.IN_PROGRESS,
                seats, players, properties,
                new TurnState(PLAYER_1, TurnPhase.WAITING_FOR_ROLL, true, false),
                null, null, null, null, null
        );
    }

    private static PlayerSnapshot player(String playerId, int seatIndex, int cash) {
        return new PlayerSnapshot(playerId, "seat-" + seatIndex, playerId, cash,
                0, false, false, false, 0, 0, List.of());
    }
}
