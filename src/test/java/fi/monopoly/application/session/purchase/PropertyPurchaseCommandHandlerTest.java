package fi.monopoly.application.session.purchase;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
import fi.monopoly.application.session.auction.AuctionCommandHandler;
import fi.monopoly.application.session.auction.AuctionGateway;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.domain.decision.PendingDecision;
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

class PropertyPurchaseCommandHandlerTest {

    @BeforeEach
    void setUp() {
        PropertyFactory.resetState();
    }

    @Test
    void buyPropertyCommandAcceptsValidDecisionAndBuysProperty() {
        Player human = new Player("Human", Color.MEDIUMPURPLE, 1500, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty property = new StreetProperty(SpotType.B1);
        var pendingDecisionRef = new AtomicReference<PendingDecision>();
        var auctionStateRef = new AtomicReference<AuctionState>();
        var continuationRef = new AtomicReference<TurnContinuationState>();
        var callbackTriggered = new AtomicBoolean(false);
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, continuationRef, gateway);

        PendingDecision decision = handler.openDecision(
                human,
                property,
                "Buy Brown 1?",
                continuationFor(human, property, TurnContinuationType.RESUME_TURN_FOLLOW_UP),
                () -> callbackTriggered.set(true)
        );

        var result = handler.handle(new BuyPropertyCommand("local-session", "player-" + human.getId(), decision.decisionId(), property.getSpotType().name()));

        assertTrue(result.accepted());
        assertEquals(human, property.getOwnerPlayer());
        assertNull(pendingDecisionRef.get());
        assertNull(continuationRef.get());
        assertTrue(callbackTriggered.get());
    }

    @Test
    void buyPropertyCommandRejectsWrongActor() {
        Player human = new Player("Human", Color.MEDIUMPURPLE, 1500, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty property = new StreetProperty(SpotType.B1);
        var pendingDecisionRef = new AtomicReference<PendingDecision>();
        var auctionStateRef = new AtomicReference<AuctionState>();
        var continuationRef = new AtomicReference<TurnContinuationState>();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, continuationRef, new FakeGateway());

        PendingDecision decision = handler.openDecision(
                human,
                property,
                "Buy Brown 1?",
                continuationFor(human, property, TurnContinuationType.RESUME_INTERACTIVE_EFFECTS),
                () -> {
                }
        );

        var result = handler.handle(new BuyPropertyCommand("local-session", "player-999", decision.decisionId(), property.getSpotType().name()));

        assertFalse(result.accepted());
        assertEquals("INVALID_PROPERTY_PURCHASE", result.rejections().get(0).code());
        assertNull(property.getOwnerPlayer());
        assertNotNull(pendingDecisionRef.get());
    }

    @Test
    void buyPropertyCommandRejectsWhenPropertyCannotBeBought() {
        Player human = new Player("Human", Color.MEDIUMPURPLE, 10, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty property = new StreetProperty(SpotType.B1);
        var pendingDecisionRef = new AtomicReference<PendingDecision>();
        var auctionStateRef = new AtomicReference<AuctionState>();
        var continuationRef = new AtomicReference<TurnContinuationState>();
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, continuationRef, gateway);

        PendingDecision decision = handler.openDecision(
                human,
                property,
                "Buy Brown 1?",
                continuationFor(human, property, TurnContinuationType.RESUME_TURN_FOLLOW_UP),
                () -> {
                }
        );

        var result = handler.handle(new BuyPropertyCommand("local-session", "player-" + human.getId(), decision.decisionId(), property.getSpotType().name()));

        assertFalse(result.accepted());
        assertEquals("PROPERTY_CANNOT_BE_BOUGHT", result.rejections().get(0).code());
        assertNull(property.getOwnerPlayer());
        assertNotNull(pendingDecisionRef.get());
    }

    @Test
    void declinePropertyCommandOpensAuctionState() {
        Player human = new Player("Human", Color.MEDIUMPURPLE, 1500, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty property = new StreetProperty(SpotType.B1);
        var pendingDecisionRef = new AtomicReference<PendingDecision>();
        var auctionStateRef = new AtomicReference<AuctionState>();
        var continuationRef = new AtomicReference<TurnContinuationState>();
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, continuationRef, gateway);

        PendingDecision decision = handler.openDecision(
                human,
                property,
                "Buy Brown 1?",
                continuationFor(human, property, TurnContinuationType.RESUME_INTERACTIVE_EFFECTS),
                () -> {
                }
        );

        var result = handler.handle(new DeclinePropertyCommand("local-session", "player-" + human.getId(), decision.decisionId(), property.getSpotType().name()));

        assertTrue(result.accepted());
        assertNull(pendingDecisionRef.get());
        assertNotNull(auctionStateRef.get());
        assertEquals(continuationRef.get(), continuationFor(human, property, TurnContinuationType.RESUME_INTERACTIVE_EFFECTS));
        assertEquals(property.getSpotType().name(), auctionStateRef.get().propertyId());
        assertEquals(AuctionStatus.ACTIVE, auctionStateRef.get().status());
    }

    private PropertyPurchaseCommandHandler newHandler(
            Player human,
            AtomicReference<PendingDecision> pendingDecisionRef,
            AtomicReference<AuctionState> auctionStateRef,
            AtomicReference<TurnContinuationState> continuationRef,
            PropertyPurchaseGateway gateway
    ) {
        return new PropertyPurchaseCommandHandler(
                "local-session",
                () -> new SessionState(
                        "local-session",
                        0L,
                        SessionStatus.IN_PROGRESS,
                        List.of(new SeatState("seat-0", 0, "player-" + human.getId(), SeatKind.HUMAN, ControlMode.MANUAL, human.getName(), "HUMAN", "#000000")),
                        List.of(new PlayerSnapshot("player-" + human.getId(), "seat-0", human.getName(), human.getMoneyAmount(), -1, false, false, false, 0, 0, List.of())),
                        List.of(),
                        new TurnState("player-" + human.getId(), pendingDecisionRef.get() != null ? TurnPhase.WAITING_FOR_DECISION : auctionStateRef.get() != null ? TurnPhase.WAITING_FOR_AUCTION : TurnPhase.WAITING_FOR_ROLL, false, false),
                        pendingDecisionRef.get(),
                        auctionStateRef.get(),
                        null,
                        null,
                        continuationRef.get(),
                        null
                ),
                pendingDecisionRef::set,
                auctionStateRef::set,
                continuationRef::set,
                gateway,
                new AuctionCommandHandler(
                        "local-session",
                        () -> new SessionState(
                                "local-session",
                                0L,
                                SessionStatus.IN_PROGRESS,
                                List.of(new SeatState("seat-0", 0, "player-" + human.getId(), SeatKind.HUMAN, ControlMode.MANUAL, human.getName(), "HUMAN", "#000000")),
                                List.of(new PlayerSnapshot("player-" + human.getId(), "seat-0", human.getName(), human.getMoneyAmount(), -1, false, false, false, 0, 0, List.of())),
                                List.of(),
                                new TurnState("player-" + human.getId(), TurnPhase.WAITING_FOR_AUCTION, false, false),
                                pendingDecisionRef.get(),
                                auctionStateRef.get(),
                                null,
                                null,
                                continuationRef.get(),
                                null
                        ),
                        auctionStateRef::set,
                        continuationRef::set,
                        new AuctionGateway() {
                            @Override
                            public List<String> eligibleBidderIds(Player triggeringPlayer, fi.monopoly.components.properties.Property property) {
                                return gateway.eligibleBidderIds(triggeringPlayer);
                            }

                            @Override
                            public Player playerById(String playerId) {
                                return ("player-" + human.getId()).equals(playerId) ? human : null;
                            }

                            @Override
                            public fi.monopoly.components.properties.Property propertyById(String propertyId) {
                                return PropertyFactory.getProperty(SpotType.valueOf(propertyId));
                            }

                            @Override
                            public int maxBidFor(Player bidder, fi.monopoly.components.properties.Property property) {
                                return 200;
                            }

                            @Override
                            public int nextBidAmount(Player bidder, fi.monopoly.components.properties.Property property, int currentBid) {
                                return currentBid == 0 ? 10 : currentBid + 10;
                            }

                            @Override
                            public boolean transferWinningProperty(Player winner, fi.monopoly.components.properties.Property property, int amount) {
                                return winner.buyProperty(property, amount);
                            }
                        }
                )
        );
    }

    private TurnContinuationState continuationFor(Player player, StreetProperty property, TurnContinuationType type) {
        return new TurnContinuationState(
                "continuation:" + player.getId() + ":" + property.getSpotType().name(),
                "player-" + player.getId(),
                type,
                type == TurnContinuationType.RESUME_TURN_FOLLOW_UP ? TurnContinuationAction.APPLY_TURN_FOLLOW_UP : TurnContinuationAction.NONE,
                property.getSpotType().name(),
                type == TurnContinuationType.RESUME_TURN_FOLLOW_UP ? "resume-turn-follow-up" : "resume-interactive-effects"
        );
    }

    private static final class FakeGateway implements PropertyPurchaseGateway {
        private boolean auctionStarted;

        @Override
        public boolean buyProperty(Player player, fi.monopoly.components.properties.Property property) {
            return player.buyProperty(property);
        }

        @Override
        public void startAuction(Player triggeringPlayer, fi.monopoly.components.properties.Property property, fi.monopoly.components.CallbackAction onComplete) {
            auctionStarted = true;
        }

        @Override
        public List<String> eligibleBidderIds(Player triggeringPlayer) {
            return List.of("player-" + triggeringPlayer.getId(), "player-2");
        }
    }
}
