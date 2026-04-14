package fi.monopoly.application.session.auction;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import fi.monopoly.application.command.SessionCommand;
import fi.monopoly.application.result.CommandRejection;
import fi.monopoly.application.result.CommandResult;
import fi.monopoly.application.result.DomainEvent;
import fi.monopoly.components.CallbackAction;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.turn.PropertyAuctionResolver;
import fi.monopoly.domain.session.AuctionState;
import fi.monopoly.domain.session.AuctionStatus;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TurnContinuationState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AuctionCommandHandler {
    private final String sessionId;
    private final Supplier<SessionState> currentStateSupplier;
    private final Consumer<AuctionState> auctionStateSetter;
    private final Consumer<TurnContinuationState> turnContinuationSetter;
    private final AuctionGateway gateway;
    private ActiveAuctionContext activeContext;

    public AuctionCommandHandler(
            String sessionId,
            Supplier<SessionState> currentStateSupplier,
            Consumer<AuctionState> auctionStateSetter,
            Consumer<TurnContinuationState> turnContinuationSetter,
            AuctionGateway gateway
    ) {
        this.sessionId = sessionId;
        this.currentStateSupplier = currentStateSupplier;
        this.auctionStateSetter = auctionStateSetter;
        this.turnContinuationSetter = turnContinuationSetter;
        this.gateway = gateway;
    }

    public AuctionState startAuction(
            Player triggeringPlayer,
            Property property,
            TurnContinuationState continuationState,
            CallbackAction onComplete
    ) {
        List<String> eligibleBidderIds = gateway.eligibleBidderIds(triggeringPlayer, property);
        if (eligibleBidderIds.isEmpty()) {
            activeContext = null;
            auctionStateSetter.accept(null);
            return null;
        }
        AuctionState state = new AuctionState(
                "auction:" + property.getSpotType().name() + ":" + (triggeringPlayer == null ? "bank" : triggeringPlayer.getId()),
                property.getSpotType().name(),
                playerId(triggeringPlayer),
                eligibleBidderIds.get(0),
                null,
                0,
                PropertyAuctionResolver.AUCTION_OPENING_BID,
                Set.of(),
                eligibleBidderIds,
                AuctionStatus.ACTIVE,
                0,
                null
        );
        activeContext = new ActiveAuctionContext(state.auctionId(), property, continuationState, onComplete);
        turnContinuationSetter.accept(continuationState);
        auctionStateSetter.accept(state);
        return state;
    }

    public boolean supports(SessionCommand command) {
        return command instanceof PlaceAuctionBidCommand
                || command instanceof PassAuctionCommand
                || command instanceof FinishAuctionResolutionCommand;
    }

    public CommandResult handle(SessionCommand command) {
        if (command instanceof PlaceAuctionBidCommand placeAuctionBidCommand) {
            return handleBid(placeAuctionBidCommand);
        }
        if (command instanceof PassAuctionCommand passAuctionCommand) {
            return handlePass(passAuctionCommand);
        }
        if (command instanceof FinishAuctionResolutionCommand finishAuctionResolutionCommand) {
            return handleFinish(finishAuctionResolutionCommand);
        }
        return reject("UNSUPPORTED_AUCTION_COMMAND", "Unsupported auction command");
    }

    public CommandResult handleComputerAction(String actorPlayerId) {
        AuctionState state = currentStateSupplier.get().auctionState();
        if (state == null || state.status() != AuctionStatus.ACTIVE) {
            return reject("NO_ACTIVE_AUCTION", "There is no active auction to resolve");
        }
        if (!Objects.equals(state.currentActorPlayerId(), actorPlayerId)) {
            return reject("WRONG_AUCTION_ACTOR", "Computer player is not the current auction actor");
        }
        Property property = activeProperty(state);
        Player actor = activePlayer(actorPlayerId);
        if (property == null || actor == null) {
            return reject("AUCTION_CONTEXT_MISSING", "Auction context is no longer available");
        }
        int maxBid = gateway.maxBidFor(actor, property);
        if (maxBid < state.minimumNextBid()) {
            return handle(new PassAuctionCommand(sessionId, actorPlayerId, state.auctionId()));
        }
        int amount = gateway.nextBidAmount(actor, property, state.currentBid());
        return handle(new PlaceAuctionBidCommand(sessionId, actorPlayerId, state.auctionId(), amount));
    }

    private CommandResult handleBid(PlaceAuctionBidCommand command) {
        AuctionState state = validateActiveAuction(command.sessionId(), command.auctionId());
        if (state == null) {
            return reject("INVALID_AUCTION", "Auction bid command is not valid in the current state");
        }
        if (state.status() != AuctionStatus.ACTIVE) {
            return reject("AUCTION_NOT_ACTIVE", "Auction is not accepting bids");
        }
        if (!Objects.equals(state.currentActorPlayerId(), command.actorPlayerId())) {
            return reject("WRONG_AUCTION_ACTOR", "Only the current auction actor can bid");
        }
        if (state.passedPlayerIds().contains(command.actorPlayerId())) {
            return reject("ACTOR_ALREADY_PASSED", "Passed players cannot re-enter the auction");
        }
        if (command.amount() < state.minimumNextBid()) {
            return reject("BID_TOO_LOW", "Bid is below the minimum next bid");
        }

        Property property = activeProperty(state);
        Player bidder = activePlayer(command.actorPlayerId());
        if (property == null || bidder == null) {
            return reject("AUCTION_CONTEXT_MISSING", "Auction context is no longer available");
        }
        int maxBid = gateway.maxBidFor(bidder, property);
        if (command.amount() > maxBid) {
            return reject("BID_TOO_HIGH", "Bid exceeds what the player can currently afford");
        }

        AuctionState updated = new AuctionState(
                state.auctionId(),
                state.propertyId(),
                state.triggeringPlayerId(),
                nextEligibleActor(state.eligiblePlayerIds(), state.passedPlayerIds(), command.actorPlayerId()),
                command.actorPlayerId(),
                command.amount(),
                command.amount() + PropertyAuctionResolver.AUCTION_BID_INCREMENT,
                state.passedPlayerIds(),
                state.eligiblePlayerIds(),
                AuctionStatus.ACTIVE,
                0,
                null
        );
        if (remainingActiveBidderIds(updated).size() <= 1) {
            updated = new AuctionState(
                    updated.auctionId(),
                    updated.propertyId(),
                    updated.triggeringPlayerId(),
                    null,
                    updated.leadingPlayerId(),
                    updated.currentBid(),
                    updated.minimumNextBid(),
                    updated.passedPlayerIds(),
                    updated.eligiblePlayerIds(),
                    AuctionStatus.WON_PENDING_RESOLUTION,
                    updated.currentBid(),
                    updated.leadingPlayerId()
            );
        }
        auctionStateSetter.accept(updated);
        List<DomainEvent> events = new ArrayList<>();
        events.add(new DomainEvent("AuctionBidPlaced", command.actorPlayerId(), "M" + command.amount()));
        if (updated.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            events.add(new DomainEvent("AuctionWon", updated.winningPlayerId(), property.getDisplayName()));
        }
        return accepted(events);
    }

    private CommandResult handlePass(PassAuctionCommand command) {
        AuctionState state = validateActiveAuction(command.sessionId(), command.auctionId());
        if (state == null) {
            return reject("INVALID_AUCTION", "Auction pass command is not valid in the current state");
        }
        if (state.status() != AuctionStatus.ACTIVE) {
            return reject("AUCTION_NOT_ACTIVE", "Auction is not accepting passes");
        }
        if (!Objects.equals(state.currentActorPlayerId(), command.actorPlayerId())) {
            return reject("WRONG_AUCTION_ACTOR", "Only the current auction actor can pass");
        }
        if (state.passedPlayerIds().contains(command.actorPlayerId())) {
            return reject("ACTOR_ALREADY_PASSED", "Player has already passed this auction");
        }

        Set<String> passedPlayerIds = new LinkedHashSet<>(state.passedPlayerIds());
        passedPlayerIds.add(command.actorPlayerId());
        Property property = activeProperty(state);
        if (property == null) {
            return reject("AUCTION_CONTEXT_MISSING", "Auction context is no longer available");
        }

        List<String> remainingActive = remainingActiveBidderIds(state.eligiblePlayerIds(), passedPlayerIds);
        if (remainingActive.isEmpty() && state.leadingPlayerId() == null) {
            ActiveAuctionContext resolvedContext = activeContext;
            activeContext = null;
            auctionStateSetter.accept(null);
            turnContinuationSetter.accept(null);
            resolvedContext.onComplete().doAction();
            return new CommandResult(
                    true,
                    currentStateSupplier.get(),
                    List.of(
                            new DomainEvent("AuctionPassed", command.actorPlayerId(), property.getDisplayName()),
                            new DomainEvent("AuctionEndedWithoutWinner", command.actorPlayerId(), property.getDisplayName())
                    ),
                    List.of(),
                    List.of()
            );
        }

        AuctionState updated;
        if (state.leadingPlayerId() != null && remainingActive.size() <= 1) {
            updated = new AuctionState(
                    state.auctionId(),
                    state.propertyId(),
                    state.triggeringPlayerId(),
                    null,
                    state.leadingPlayerId(),
                    state.currentBid(),
                    state.minimumNextBid(),
                    passedPlayerIds,
                    state.eligiblePlayerIds(),
                    AuctionStatus.WON_PENDING_RESOLUTION,
                    state.currentBid(),
                    state.leadingPlayerId()
            );
        } else {
            updated = new AuctionState(
                    state.auctionId(),
                    state.propertyId(),
                    state.triggeringPlayerId(),
                    nextEligibleActor(state.eligiblePlayerIds(), passedPlayerIds, command.actorPlayerId()),
                    state.leadingPlayerId(),
                    state.currentBid(),
                    state.minimumNextBid(),
                    passedPlayerIds,
                    state.eligiblePlayerIds(),
                    AuctionStatus.ACTIVE,
                    0,
                    null
            );
        }
        auctionStateSetter.accept(updated);
        List<DomainEvent> events = new ArrayList<>();
        events.add(new DomainEvent("AuctionPassed", command.actorPlayerId(), property.getDisplayName()));
        if (updated.status() == AuctionStatus.WON_PENDING_RESOLUTION) {
            events.add(new DomainEvent("AuctionWon", updated.winningPlayerId(), property.getDisplayName()));
        }
        return accepted(events);
    }

    private CommandResult handleFinish(FinishAuctionResolutionCommand command) {
        AuctionState state = validateActiveAuction(command.sessionId(), command.auctionId());
        if (state == null) {
            return reject("INVALID_AUCTION", "Auction resolution command is not valid in the current state");
        }
        if (state.status() != AuctionStatus.WON_PENDING_RESOLUTION) {
            return reject("AUCTION_NOT_READY", "Auction is not waiting for final resolution");
        }
        Property property = activeProperty(state);
        Player winner = activePlayer(state.winningPlayerId());
        if (property == null || winner == null) {
            return reject("AUCTION_CONTEXT_MISSING", "Auction context is no longer available");
        }
        if (!gateway.transferWinningProperty(winner, property, state.winningBid())) {
            return reject("AUCTION_TRANSFER_FAILED", "Winning property transfer failed");
        }
        PropertyAuctionResolver.recordAuctionOutcome(property, state.winningBid());
        ActiveAuctionContext resolvedContext = activeContext;
        activeContext = null;
        auctionStateSetter.accept(null);
        turnContinuationSetter.accept(null);
        resolvedContext.onComplete().doAction();
        return new CommandResult(
                true,
                currentStateSupplier.get(),
                List.of(new DomainEvent("AuctionResolved", state.winningPlayerId(), property.getDisplayName())),
                List.of(),
                List.of()
        );
    }

    private AuctionState validateActiveAuction(String commandSessionId, String auctionId) {
        if (!Objects.equals(sessionId, commandSessionId) || activeContext == null) {
            return null;
        }
        AuctionState state = currentStateSupplier.get().auctionState();
        if (state == null || !Objects.equals(state.auctionId(), auctionId) || !Objects.equals(activeContext.auctionId(), auctionId)) {
            return null;
        }
        return state;
    }

    private Property activeProperty(AuctionState state) {
        if (activeContext != null && Objects.equals(activeContext.auctionId(), state.auctionId())) {
            return activeContext.property();
        }
        return gateway.propertyById(state.propertyId());
    }

    private Player activePlayer(String playerId) {
        return playerId == null ? null : gateway.playerById(playerId);
    }

    private List<String> remainingActiveBidderIds(AuctionState state) {
        return remainingActiveBidderIds(state.eligiblePlayerIds(), state.passedPlayerIds());
    }

    private List<String> remainingActiveBidderIds(List<String> eligiblePlayerIds, Set<String> passedPlayerIds) {
        List<String> remaining = new ArrayList<>();
        for (String playerId : eligiblePlayerIds) {
            if (!passedPlayerIds.contains(playerId)) {
                remaining.add(playerId);
            }
        }
        return remaining;
    }

    private String nextEligibleActor(List<String> eligiblePlayerIds, Set<String> passedPlayerIds, String currentActorPlayerId) {
        if (eligiblePlayerIds.isEmpty()) {
            return null;
        }
        int currentIndex = eligiblePlayerIds.indexOf(currentActorPlayerId);
        if (currentIndex < 0) {
            currentIndex = -1;
        }
        for (int offset = 1; offset <= eligiblePlayerIds.size(); offset++) {
            String candidate = eligiblePlayerIds.get((currentIndex + offset) % eligiblePlayerIds.size());
            if (!passedPlayerIds.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private CommandResult accepted(List<DomainEvent> events) {
        return new CommandResult(true, currentStateSupplier.get(), events, List.of(), List.of());
    }

    private CommandResult reject(String code, String message) {
        return new CommandResult(false, currentStateSupplier.get(), List.of(), List.of(new CommandRejection(code, message)), List.of());
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }

    private record ActiveAuctionContext(
            String auctionId,
            Property property,
            TurnContinuationState continuationState,
            CallbackAction onComplete
    ) {
    }
}
