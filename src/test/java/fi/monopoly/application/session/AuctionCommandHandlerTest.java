package fi.monopoly.application.session;

import fi.monopoly.application.command.FinishAuctionResolutionCommand;
import fi.monopoly.application.command.PassAuctionCommand;
import fi.monopoly.application.command.PlaceAuctionBidCommand;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class AuctionCommandHandlerTest {

    @BeforeEach
    void setUp() {
        PropertyFactory.resetState();
    }

    @Test
    void bidRejectsWrongActor() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> {
        });

        var result = handler.handle(new PlaceAuctionBidCommand("local-session", players.secondActorId(), state.auctionId(), 10));

        assertFalse(result.accepted());
        assertEquals("WRONG_AUCTION_ACTOR", result.rejections().get(0).code());
    }

    @Test
    void bidRejectsBelowMinimumBid() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> {
        });

        var result = handler.handle(new PlaceAuctionBidCommand("local-session", state.currentActorPlayerId(), state.auctionId(), 5));

        assertFalse(result.accepted());
        assertEquals("BID_TOO_LOW", result.rejections().get(0).code());
    }

    @Test
    void passAdvancesToNextEligibleActor() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> {
        });

        var result = handler.handle(new PassAuctionCommand("local-session", state.currentActorPlayerId(), state.auctionId()));

        assertTrue(result.accepted());
        assertEquals(players.secondActorId(), auctionStateRef.get().currentActorPlayerId());
        assertTrue(auctionStateRef.get().passedPlayerIds().contains(state.currentActorPlayerId()));
    }

    @Test
    void passCanProduceWinningPendingResolution() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> {
        });
        assertTrue(handler.handle(new PlaceAuctionBidCommand("local-session", state.currentActorPlayerId(), state.auctionId(), 10)).accepted());
        AuctionState afterBid = auctionStateRef.get();

        var result = handler.handle(new PassAuctionCommand("local-session", afterBid.currentActorPlayerId(), afterBid.auctionId()));

        assertTrue(result.accepted());
        assertEquals(AuctionStatus.WON_PENDING_RESOLUTION, auctionStateRef.get().status());
        assertEquals(afterBid.leadingPlayerId(), auctionStateRef.get().winningPlayerId());
        assertEquals(10, auctionStateRef.get().winningBid());
    }

    @Test
    void noBidAuctionClearsStateAndRunsCompletion() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> completed.set(true));
        assertTrue(handler.handle(new PassAuctionCommand("local-session", state.currentActorPlayerId(), state.auctionId())).accepted());
        AuctionState afterFirstPass = auctionStateRef.get();

        var result = handler.handle(new PassAuctionCommand("local-session", afterFirstPass.currentActorPlayerId(), afterFirstPass.auctionId()));

        assertTrue(result.accepted());
        assertNull(auctionStateRef.get());
        assertTrue(completed.get());
    }

    @Test
    void finishResolutionTransfersPropertyAndClearsAuction() {
        TestPlayers players = testPlayers();
        AtomicReference<AuctionState> auctionStateRef = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AuctionCommandHandler handler = newHandler(players, auctionStateRef);
        StreetProperty property = new StreetProperty(SpotType.B1);

        AuctionState state = handler.startAuction(players.triggeringPlayer(), property, () -> completed.set(true));
        assertTrue(handler.handle(new PlaceAuctionBidCommand("local-session", state.currentActorPlayerId(), state.auctionId(), 10)).accepted());
        AuctionState afterBid = auctionStateRef.get();
        assertTrue(handler.handle(new PassAuctionCommand("local-session", afterBid.currentActorPlayerId(), afterBid.auctionId())).accepted());
        AuctionState wonState = auctionStateRef.get();
        assertEquals(AuctionStatus.WON_PENDING_RESOLUTION, wonState.status());

        var result = handler.handle(new FinishAuctionResolutionCommand("local-session", wonState.auctionId()));

        assertTrue(result.accepted());
        assertEquals(players.firstBidder(), property.getOwnerPlayer());
        assertNull(auctionStateRef.get());
        assertTrue(completed.get());
    }

    private AuctionCommandHandler newHandler(TestPlayers players, AtomicReference<AuctionState> auctionStateRef) {
        return new AuctionCommandHandler(
                "local-session",
                () -> new SessionState(
                        "local-session",
                        0L,
                        SessionStatus.IN_PROGRESS,
                        List.of(
                                new SeatState("seat-0", 0, players.triggeringPlayerId(), SeatKind.HUMAN, ControlMode.MANUAL, players.triggeringPlayer().getName()),
                                new SeatState("seat-1", 1, players.firstBidderId(), SeatKind.HUMAN, ControlMode.MANUAL, players.firstBidder().getName()),
                                new SeatState("seat-2", 2, players.secondActorId(), SeatKind.HUMAN, ControlMode.MANUAL, players.secondBidder().getName())
                        ),
                        List.of(
                                snapshot(players.triggeringPlayer()),
                                snapshot(players.firstBidder()),
                                snapshot(players.secondBidder())
                        ),
                        new TurnState(players.triggeringPlayerId(), auctionStateRef.get() == null ? TurnPhase.WAITING_FOR_ROLL : TurnPhase.WAITING_FOR_AUCTION, false, false),
                        null,
                        auctionStateRef.get(),
                        null,
                        null
                ),
                auctionStateRef::set,
                new FakeAuctionGateway(players)
        );
    }

    private PlayerSnapshot snapshot(Player player) {
        return new PlayerSnapshot(
                "player-" + player.getId(),
                "seat-" + (player.getTurnNumber() - 1),
                player.getName(),
                player.getMoneyAmount(),
                -1,
                false,
                false,
                false,
                0,
                List.of()
        );
    }

    private TestPlayers testPlayers() {
        Player triggering = new Player("Trigger", Color.GOLDENROD, 1500, 1, ComputerPlayerProfile.HUMAN);
        Player firstBidder = new Player("BidderOne", Color.DARKCYAN, 1500, 2, ComputerPlayerProfile.HUMAN);
        Player secondBidder = new Player("BidderTwo", Color.DARKMAGENTA, 1500, 3, ComputerPlayerProfile.HUMAN);
        return new TestPlayers(triggering, firstBidder, secondBidder);
    }

    private record TestPlayers(Player triggeringPlayer, Player firstBidder, Player secondBidder) {
        private String triggeringPlayerId() {
            return "player-" + triggeringPlayer.getId();
        }

        private String firstBidderId() {
            return "player-" + firstBidder.getId();
        }

        private String secondActorId() {
            return "player-" + secondBidder.getId();
        }
    }

    private static final class FakeAuctionGateway implements AuctionGateway {
        private final TestPlayers players;

        private FakeAuctionGateway(TestPlayers players) {
            this.players = players;
        }

        @Override
        public List<String> eligibleBidderIds(Player triggeringPlayer, Property property) {
            return List.of(players.firstBidderId(), players.secondActorId());
        }

        @Override
        public Player playerById(String playerId) {
            if (players.triggeringPlayerId().equals(playerId)) {
                return players.triggeringPlayer();
            }
            if (players.firstBidderId().equals(playerId)) {
                return players.firstBidder();
            }
            if (players.secondActorId().equals(playerId)) {
                return players.secondBidder();
            }
            return null;
        }

        @Override
        public Property propertyById(String propertyId) {
            return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
        }

        @Override
        public int maxBidFor(Player bidder, Property property) {
            return 200;
        }

        @Override
        public int nextBidAmount(Player bidder, Property property, int currentBid) {
            return currentBid == 0 ? 10 : currentBid + 10;
        }

        @Override
        public boolean transferWinningProperty(Player winner, Property property, int amount) {
            return winner.buyProperty(property, amount);
        }
    }
}
