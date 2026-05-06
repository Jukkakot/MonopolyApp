package fi.monopoly.server.session;

import fi.monopoly.application.command.*;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.domain.session.PropertyStateSnapshot;
import fi.monopoly.application.session.SessionApplicationService;
import fi.monopoly.domain.decision.DecisionPayload;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.decision.PropertyPurchaseDecisionPayload;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that a full game can be driven through the pure domain path without deadlocking.
 *
 * <p>Uses a greedy agent: always buys properties when affordable, always pays debt when
 * cash allows, declares bankruptcy otherwise. No Processing runtime involved.</p>
 */
class PureDomainGameSimulationTest {

    private static final String SESSION_ID = "sim-session";
    private static final int MAX_STEPS = 2_000;
    private static final int MIN_TURN_SWITCHES = 20;

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void twoPlayerGameCompletesOrProgressesBeyondMinTurnThreshold() {
        SessionState initial = PureDomainSessionFactory.initialGameState(
                SESSION_ID, List.of("Eka", "Toka"), List.of("#E63946", "#2A9D8F"), new Random(42));
        SessionApplicationService service = PureDomainSessionFactory.create(SESSION_ID, initial);

        SimulationResult result = runSimulation(service);

        assertFalse(result.stalled(),
                "Pure domain simulation stalled after " + result.steps() + " steps without progress");
        assertTrue(result.turnSwitches() >= MIN_TURN_SWITCHES || result.gameOver(),
                "Expected at least " + MIN_TURN_SWITCHES + " turn switches or game over; got "
                        + result.turnSwitches() + " switches, gameOver=" + result.gameOver());
    }

    @RepeatedTest(5)
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void twoPlayerGameAlwaysProgressesAcrossSeeds() {
        long seed = System.nanoTime();
        SessionState initial = PureDomainSessionFactory.initialGameState(
                SESSION_ID, List.of("A", "B"), List.of("#F00", "#0F0"), new Random(seed));
        SessionApplicationService service = PureDomainSessionFactory.create(SESSION_ID, initial);

        SimulationResult result = runSimulation(service);

        assertFalse(result.stalled(),
                "Simulation stalled (seed=" + seed + ") after " + result.steps() + " steps");
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void fourPlayerGameCompletesOrProgressesBeyondMinTurnThreshold() {
        SessionState initial = PureDomainSessionFactory.initialGameState(
                SESSION_ID, List.of("A", "B", "C", "D"),
                List.of("#F00", "#0F0", "#00F", "#FF0"), new Random(7));
        SessionApplicationService service = PureDomainSessionFactory.create(SESSION_ID, initial);

        SimulationResult result = runSimulation(service);

        assertFalse(result.stalled(),
                "4-player simulation stalled after " + result.steps() + " steps");
        assertTrue(result.turnSwitches() >= MIN_TURN_SWITCHES || result.gameOver(),
                "Expected progress in 4-player game; turns=" + result.turnSwitches());
    }

    // -------------------------------------------------------------------------
    // Simulation engine
    // -------------------------------------------------------------------------

    private SimulationResult runSimulation(SessionApplicationService service) {
        int steps = 0;
        int turnSwitches = 0;
        int rejectedConsecutive = 0;
        String lastActivePlayer = null;
        boolean gameOver = false;

        while (steps < MAX_STEPS) {
            SessionState state = service.currentState();

            if (state.status() == SessionStatus.GAME_OVER) {
                gameOver = true;
                break;
            }

            String activeId = state.turn() != null ? state.turn().activePlayerId() : null;
            if (activeId != null && !activeId.equals(lastActivePlayer)) {
                turnSwitches++;
                lastActivePlayer = activeId;
            }

            CommandResult result = dispatchGreedy(service, state, activeId);
            if (!result.accepted()) {
                rejectedConsecutive++;
                if (rejectedConsecutive == 5) {
                    // Log state to help diagnose deadlock
                    System.err.printf("[sim-debug] Consecutive rejections=%d step=%d phase=%s activeId=%s auctionState=%s debt=%s pendingDecision=%s%n",
                            rejectedConsecutive, steps,
                            state.turn() != null ? state.turn().phase() : "null",
                            activeId,
                            state.auctionState() != null ? "bid=" + state.auctionState().currentBid() + " actor=" + state.auctionState().currentActorPlayerId() : "null",
                            state.activeDebt() != null ? "amount=" + state.activeDebt().amountRemaining() : "null",
                            state.pendingDecision() != null ? state.pendingDecision().decisionId() : "null");
                }
                // Ten consecutive rejections = genuine deadlock (no valid command exists)
                if (rejectedConsecutive >= 10) {
                    return new SimulationResult(steps, turnSwitches, true, false);
                }
            } else {
                rejectedConsecutive = 0;
            }
            steps++;
        }

        return new SimulationResult(steps, turnSwitches, false, gameOver);
    }

    private CommandResult dispatchGreedy(SessionApplicationService service, SessionState state, String activeId) {
        if (activeId == null) {
            return new CommandResult(false, state, List.of(),
                    List.of(new CommandRejection("no-active-player", null)), List.of());
        }
        TurnPhase phase = state.turn() != null ? state.turn().phase() : null;
        if (phase == null) {
            return new CommandResult(false, state, List.of(),
                    List.of(new CommandRejection("null-phase", null)), List.of());
        }
        return switch (phase) {
            case WAITING_FOR_ROLL -> service.handle(new RollDiceCommand(SESSION_ID, activeId));
            case WAITING_FOR_END_TURN -> service.handle(new EndTurnCommand(SESSION_ID, activeId));
            case WAITING_FOR_DECISION -> handleDecision(service, state, activeId);
            case RESOLVING_DEBT -> handleDebt(service, state, activeId);
            case WAITING_FOR_AUCTION -> handleAuction(service, state);
            default -> new CommandResult(false, state, List.of(),
                    List.of(new CommandRejection("unhandled-phase:" + phase, null)), List.of());
        };
    }

    private CommandResult handleDecision(SessionApplicationService service, SessionState state, String activeId) {
        PendingDecision decision = state.pendingDecision();
        if (decision == null) {
            return service.handle(new EndTurnCommand(SESSION_ID, activeId));
        }
        DecisionPayload payload = decision.payload();
        if (payload instanceof PropertyPurchaseDecisionPayload purchase) {
            PlayerSnapshot player = findPlayer(state, activeId);
            int cash = player != null ? player.cash() : 0;
            // Buy if affordable; otherwise go to auction (decline triggers auction in domain path)
            if (cash >= purchase.price()) {
                return service.handle(new BuyPropertyCommand(
                        SESSION_ID, activeId, decision.decisionId(), purchase.propertyId()));
            }
            return service.handle(new DeclinePropertyCommand(
                    SESSION_ID, activeId, decision.decisionId(), purchase.propertyId()));
        }
        return service.handle(new EndTurnCommand(SESSION_ID, activeId));
    }

    private CommandResult handleDebt(SessionApplicationService service, SessionState state, String activeId) {
        DebtStateModel debt = state.activeDebt();
        if (debt == null) {
            return service.handle(new EndTurnCommand(SESSION_ID, activeId));
        }
        String debtorId = debt.debtorPlayerId();
        // Use debt.currentCash() for consistency with handler validation (same cash snapshot)
        if (debt.currentCash() >= debt.amountRemaining()) {
            return service.handle(new PayDebtCommand(SESSION_ID, debtorId, debt.debtId()));
        }
        if (debt.bankruptcyRisk()) {
            return service.handle(new DeclareBankruptcyCommand(SESSION_ID, debtorId, debt.debtId()));
        }
        // Can't pay directly and not yet bankruptcy-eligible → mortgage an unmortgaged property
        return state.properties().stream()
                .filter(p -> debtorId.equals(p.ownerPlayerId()) && !p.mortgaged())
                .findFirst()
                .map(p -> service.handle(new MortgagePropertyForDebtCommand(SESSION_ID, debtorId, debt.debtId(), p.propertyId())))
                .orElseGet(() -> service.handle(new DeclareBankruptcyCommand(SESSION_ID, debtorId, debt.debtId())));
    }

    private CommandResult handleAuction(SessionApplicationService service, SessionState state) {
        AuctionState auction = state.auctionState();
        if (auction == null) {
            return new CommandResult(false, state, List.of(),
                    List.of(new CommandRejection("no-auction-state", null)), List.of());
        }
        // Auction finished but awaiting resolution confirmation
        if (auction.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            return service.handle(new FinishAuctionResolutionCommand(SESSION_ID, auction.auctionId()));
        }
        String bidderId = auction.currentActorPlayerId();
        if (bidderId == null) {
            return new CommandResult(false, state, List.of(),
                    List.of(new CommandRejection("no-auction-bidder", null)), List.of());
        }
        PlayerSnapshot bidder = findPlayer(state, bidderId);
        int cash = bidder != null ? bidder.cash() : 0;
        int minBid = auction.minimumNextBid();
        // Bid minimum if affordable and property has value; otherwise pass
        if (cash >= minBid && minBid > 0) {
            return service.handle(new PlaceAuctionBidCommand(SESSION_ID, bidderId, auction.auctionId(), minBid));
        }
        return service.handle(new PassAuctionCommand(SESSION_ID, bidderId, auction.auctionId()));
    }

    private static PlayerSnapshot findPlayer(SessionState state, String playerId) {
        return state.players().stream()
                .filter(p -> playerId.equals(p.playerId()))
                .findFirst().orElse(null);
    }

    private record SimulationResult(int steps, int turnSwitches, boolean stalled, boolean gameOver) {}
}
