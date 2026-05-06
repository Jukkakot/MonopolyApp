package fi.monopoly.server.session;

import fi.monopoly.application.command.*;
import fi.monopoly.client.session.ClientSessionListener;
import fi.monopoly.client.session.ClientSessionSnapshot;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Server-side greedy bot driver for pure-domain sessions.
 *
 * <p>Registers as a {@link ClientSessionListener} on the provided {@link SessionCommandPublisher}.
 * When a state snapshot arrives and the active actor is a bot seat, schedules a delayed command
 * via a {@link ScheduledExecutorService} — giving the client a brief window to observe the state
 * before the bot responds. The greedy strategy mirrors {@code PureDomainGameSimulationTest}:
 * always buy when affordable, pay debt when possible, mortgage otherwise, declare bankruptcy last.</p>
 *
 * <p>Thread safety: {@code pendingAction} is an {@link AtomicBoolean} to prevent double-scheduling.
 * All commands go through the synchronized {@link SessionCommandPublisher#handle} method.</p>
 */
@Slf4j
public final class PureDomainBotDriver implements ClientSessionListener {

    private static final long BOT_THINK_DELAY_MS = 600;

    private final SessionCommandPublisher publisher;
    private final String sessionId;
    private final Set<String> botPlayerIds;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean pendingAction = new AtomicBoolean(false);

    private PureDomainBotDriver(SessionCommandPublisher publisher, String sessionId, Set<String> botPlayerIds) {
        this.publisher = publisher;
        this.sessionId = sessionId;
        this.botPlayerIds = botPlayerIds;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("bot-driver-" + sessionId.substring(0, 8), 0).factory());
    }

    /**
     * Creates and registers a {@link PureDomainBotDriver} for any BOT seats in the given state.
     * Returns {@code null} if the session has no bot seats.
     */
    static PureDomainBotDriver createAndRegisterIfNeeded(SessionCommandPublisher publisher, SessionState initialState) {
        Set<String> botIds = collectBotPlayerIds(initialState);
        if (botIds.isEmpty()) {
            return null;
        }
        PureDomainBotDriver driver = new PureDomainBotDriver(publisher, initialState.sessionId(), botIds);
        publisher.addListener(driver);
        log.info("Bot driver registered for session {} — bots: {}", initialState.sessionId().substring(0, 8), botIds);
        return driver;
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    // -------------------------------------------------------------------------
    // ClientSessionListener
    // -------------------------------------------------------------------------

    @Override
    public void onSnapshotChanged(ClientSessionSnapshot snapshot) {
        SessionState state = snapshot.state();
        if (state == null || state.status() == SessionStatus.GAME_OVER) {
            return;
        }
        if (!needsBotAction(state)) {
            return;
        }
        if (!pendingAction.compareAndSet(false, true)) {
            return;
        }
        scheduler.schedule(this::takeStep, BOT_THINK_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    // -------------------------------------------------------------------------
    // Bot step
    // -------------------------------------------------------------------------

    private void takeStep() {
        pendingAction.set(false);
        SessionState state = publisher.currentState();
        if (state == null || state.status() == SessionStatus.GAME_OVER) {
            return;
        }
        if (!needsBotAction(state)) {
            return;
        }
        dispatchGreedy(state);
    }

    private boolean needsBotAction(SessionState state) {
        String actorId = resolveActorId(state);
        return actorId != null && botPlayerIds.contains(actorId);
    }

    private String resolveActorId(SessionState state) {
        if (state.activeDebt() != null) {
            return state.activeDebt().debtorPlayerId();
        }
        if (state.auctionState() != null) {
            return state.auctionState().currentActorPlayerId();
        }
        if (state.turn() == null) return null;
        return state.turn().activePlayerId();
    }

    private void dispatchGreedy(SessionState state) {
        TurnPhase phase = state.turn() != null ? state.turn().phase() : TurnPhase.UNKNOWN;
        String activeId = resolveActorId(state);
        if (activeId == null) return;

        switch (phase) {
            case WAITING_FOR_ROLL -> publisher.handle(new RollDiceCommand(sessionId, activeId));
            case WAITING_FOR_END_TURN -> publisher.handle(new EndTurnCommand(sessionId, activeId));
            case WAITING_FOR_DECISION -> handleDecision(state, activeId);
            case RESOLVING_DEBT -> handleDebt(state);
            case WAITING_FOR_AUCTION -> handleAuction(state);
            default -> log.debug("Bot driver: unhandled phase {} for player {}", phase, activeId);
        }
    }

    private void handleDecision(SessionState state, String activeId) {
        PendingDecision decision = state.pendingDecision();
        if (decision == null) {
            publisher.handle(new EndTurnCommand(sessionId, activeId));
            return;
        }
        if (decision.payload() instanceof PropertyPurchaseDecisionPayload purchase) {
            PlayerSnapshot player = findPlayer(state, activeId);
            int cash = player != null ? player.cash() : 0;
            if (cash >= purchase.price()) {
                publisher.handle(new BuyPropertyCommand(sessionId, activeId, decision.decisionId(), purchase.propertyId()));
            } else {
                publisher.handle(new DeclinePropertyCommand(sessionId, activeId, decision.decisionId(), purchase.propertyId()));
            }
        } else {
            publisher.handle(new EndTurnCommand(sessionId, activeId));
        }
    }

    private void handleDebt(SessionState state) {
        DebtStateModel debt = state.activeDebt();
        if (debt == null) return;
        String debtorId = debt.debtorPlayerId();
        List<DebtAction> allowed = debt.allowedActions();

        if (allowed.contains(DebtAction.PAY_DEBT_NOW) && debt.currentCash() >= debt.amountRemaining()) {
            publisher.handle(new PayDebtCommand(sessionId, debtorId, debt.debtId()));
            return;
        }
        if (allowed.contains(DebtAction.SELL_BUILDING)) {
            state.properties().stream()
                    .filter(p -> debtorId.equals(p.ownerPlayerId()) && (p.houseCount() > 0 || p.hotelCount() > 0))
                    .findFirst()
                    .ifPresent(p -> publisher.handle(
                            new SellBuildingForDebtCommand(sessionId, debtorId, debt.debtId(), p.propertyId(), 1)));
            return;
        }
        if (allowed.contains(DebtAction.MORTGAGE_PROPERTY)) {
            state.properties().stream()
                    .filter(p -> debtorId.equals(p.ownerPlayerId()) && !p.mortgaged())
                    .findFirst()
                    .ifPresent(p -> publisher.handle(
                            new MortgagePropertyForDebtCommand(sessionId, debtorId, debt.debtId(), p.propertyId())));
            return;
        }
        if (allowed.contains(DebtAction.DECLARE_BANKRUPTCY)) {
            publisher.handle(new DeclareBankruptcyCommand(sessionId, debtorId, debt.debtId()));
        }
    }

    private void handleAuction(SessionState state) {
        AuctionState auction = state.auctionState();
        if (auction == null) return;
        if (auction.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            publisher.handle(new FinishAuctionResolutionCommand(sessionId, auction.auctionId()));
            return;
        }
        String bidderId = auction.currentActorPlayerId();
        if (bidderId == null || !botPlayerIds.contains(bidderId)) return;
        PlayerSnapshot bidder = findPlayer(state, bidderId);
        int cash = bidder != null ? bidder.cash() : 0;
        int minBid = auction.minimumNextBid();
        if (cash >= minBid && minBid > 0) {
            publisher.handle(new PlaceAuctionBidCommand(sessionId, bidderId, auction.auctionId(), minBid));
        } else {
            publisher.handle(new PassAuctionCommand(sessionId, bidderId, auction.auctionId()));
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Set<String> collectBotPlayerIds(SessionState state) {
        Set<String> ids = new java.util.HashSet<>();
        for (SeatState seat : state.seats()) {
            if (seat.seatKind() == SeatKind.BOT) {
                ids.add(seat.playerId());
            }
        }
        return ids.isEmpty() ? Set.of() : Set.copyOf(ids);
    }

    private static PlayerSnapshot findPlayer(SessionState state, String playerId) {
        return state.players().stream()
                .filter(p -> playerId.equals(p.playerId()))
                .findFirst().orElse(null);
    }
}
