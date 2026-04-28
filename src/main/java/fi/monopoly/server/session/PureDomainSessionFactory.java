package fi.monopoly.server.session;

import fi.monopoly.application.session.InMemorySessionState;
import fi.monopoly.application.session.OverlaySessionStateStore;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.auction.DomainAuctionGateway;
import fi.monopoly.application.session.debt.DomainDebtRemediationGateway;
import fi.monopoly.application.session.purchase.DomainPropertyPurchaseGateway;
import fi.monopoly.application.session.trade.DomainTradeGateway;
import fi.monopoly.application.session.turn.DomainTurnActionGateway;
import fi.monopoly.application.session.turn.DomainTurnContinuationGateway;
import fi.monopoly.application.session.turn.CardDeckLoader;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Creates a {@link SessionApplicationService} wired with pure domain gateway implementations.
 *
 * <p>This factory does not depend on any Processing runtime objects. It is the foundation
 * for running a standalone session server (see {@link StartSessionServer}).</p>
 *
 * <h2>Current limitations</h2>
 * <ul>
 *   <li>Bot bidding strategy is simplified (max bid = cash) vs. the legacy strategy which
 *       applies property-valuation multipliers and per-bot reserves.</li>
 *   <li>{@code DebtOpeningGateway} / {@code SessionPaymentPort} still use legacy
 *       {@code PaymentRequest}; the desktop host wires these separately for the legacy path.</li>
 * </ul>
 */
public final class PureDomainSessionFactory {

    private PureDomainSessionFactory() {}

    /**
     * Creates a fully wired {@link SessionApplicationService} with the given initial state.
     *
     * <p>Gateway implementations that are not yet pure-domain are left unconfigured; commands
     * routed to those handlers will be rejected with {@code UNSUPPORTED_COMMAND}.</p>
     */
    public static SessionApplicationService create(String sessionId, SessionState initialState) {
        InMemorySessionState store = new InMemorySessionState(initialState);
        OverlaySessionStateStore overlay = new OverlaySessionStateStore(store::get);

        SessionApplicationService service = new SessionApplicationService(sessionId, overlay);
        service.configureAuctionFlow(new DomainAuctionGateway(store));
        service.configurePropertyPurchaseFlow(new DomainPropertyPurchaseGateway(store));

        DomainTurnContinuationGateway continuationGateway = new DomainTurnContinuationGateway(store);
        service.configureTurnContinuationFlow(continuationGateway);

        DomainTurnActionGateway turnActionGateway = new DomainTurnActionGateway(
                store,
                (playerId, propertyId, displayName, price, message, continuation) ->
                        service.openPropertyPurchaseDecision(playerId, propertyId, displayName, price, message, continuation)
        );
        service.configureTurnActionFlow(turnActionGateway);

        service.configureDebtRemediationFlow(new DomainDebtRemediationGateway(store));
        service.configureTradeFlow(new DomainTradeGateway(store));

        return service;
    }

    /**
     * Builds a playable initial game state for the given player names.
     *
     * <p>Each player receives €1500 starting cash and a seat in the order provided.
     * All purchasable board properties start unowned. The first player's turn begins at GO.</p>
     *
     * @param sessionId   the session identifier
     * @param playerNames ordered list of player display names (2–4 players)
     * @param colors      ordered list of player colour hex strings (e.g. "#FF0000")
     */
    public static SessionState initialGameState(String sessionId, List<String> playerNames, List<String> colors) {
        if (playerNames.isEmpty()) throw new IllegalArgumentException("At least one player is required");

        List<PropertyStateSnapshot> properties = SpotType.SPOT_TYPES.stream()
                .filter(s -> s.isProperty)
                .map(s -> new PropertyStateSnapshot(s.name(), null, false, 0, 0))
                .toList();

        List<SeatState> seats = new ArrayList<>();
        List<PlayerSnapshot> players = new ArrayList<>();
        for (int i = 0; i < playerNames.size(); i++) {
            String name = playerNames.get(i);
            String color = i < colors.size() ? colors.get(i) : "#AAAAAA";
            String playerId = "player-" + (i + 1);
            String seatId = "seat-" + i;
            seats.add(new SeatState(seatId, i, playerId, SeatKind.HUMAN, ControlMode.MANUAL, name, "HUMAN", color));
            players.add(new PlayerSnapshot(playerId, seatId, name, 1500, 0, false, false, false, 0, 0, List.of()));
        }

        String firstPlayerId = players.get(0).playerId();
        Random rng = new Random();
        List<String> chanceDeck = CardDeckLoader.buildDeck("chance", rng);
        List<String> communityDeck = CardDeckLoader.buildDeck("community", rng);
        return SessionState.builder()
                .sessionId(sessionId)
                .version(0L)
                .status(SessionStatus.IN_PROGRESS)
                .seats(seats)
                .players(players)
                .properties(properties)
                .turn(new TurnState(firstPlayerId, TurnPhase.WAITING_FOR_ROLL, true, false, 0))
                .chanceDeck(chanceDeck)
                .communityDeck(communityDeck)
                .build();
    }

    /**
     * Builds a minimal valid starting state for a session with no players and no properties.
     *
     * <p>Suitable for tests and smoke runs.</p>
     */
    public static SessionState emptyInitialState(String sessionId) {
        return new SessionState(
                sessionId, 0L, SessionStatus.IN_PROGRESS,
                List.of(), List.of(), List.of(),
                new TurnState(null, TurnPhase.WAITING_FOR_ROLL, false, false, 0),
                null, null, null, null, null, null
        );
    }
}
