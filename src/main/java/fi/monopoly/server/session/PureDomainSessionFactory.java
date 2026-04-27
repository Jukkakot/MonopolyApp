package fi.monopoly.server.session;

import fi.monopoly.application.session.InMemorySessionState;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.application.session.auction.DomainAuctionGateway;
import fi.monopoly.application.session.purchase.DomainPropertyPurchaseGateway;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;

import java.util.List;

/**
 * Creates a {@link SessionApplicationService} wired with pure domain gateway implementations.
 *
 * <p>This factory does not depend on any Processing runtime objects. It is the foundation
 * for running a standalone session server (see {@link StartSessionServer}).</p>
 *
 * <h2>Current limitations</h2>
 * <ul>
 *   <li>Debt remediation, trade, and turn-action flows are not yet configured — those gateways
 *       still depend on legacy types ({@code DebtState}, {@code TradeOffer}, etc.) and must be
 *       extracted before the factory can wire them.</li>
 *   <li>Bot bidding strategy is simplified (max bid = cash) vs. the legacy strategy which
 *       applies property-valuation multipliers and per-bot reserves.</li>
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

        SessionApplicationService service = new SessionApplicationService(sessionId, store::get);
        service.configureAuctionFlow(new DomainAuctionGateway(store));
        service.configurePropertyPurchaseFlow(new DomainPropertyPurchaseGateway(store));
        return service;
    }

    /**
     * Builds a minimal valid starting state for a session with no players and no properties.
     *
     * <p>Suitable for tests and smoke runs. A real game start requires seats, player snapshots,
     * and property snapshots populated from the board setup.</p>
     */
    public static SessionState emptyInitialState(String sessionId) {
        return new SessionState(
                sessionId,
                0L,
                SessionStatus.IN_PROGRESS,
                List.of(),
                List.of(),
                List.of(),
                new TurnState(null, TurnPhase.WAITING_FOR_ROLL, false, false),
                null,
                null,
                null,
                null,
                null
        );
    }
}
