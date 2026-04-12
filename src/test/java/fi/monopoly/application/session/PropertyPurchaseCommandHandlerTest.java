package fi.monopoly.application.session;

import fi.monopoly.application.command.BuyPropertyCommand;
import fi.monopoly.application.command.DeclinePropertyCommand;
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
        var callbackTriggered = new AtomicBoolean(false);
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, gateway);

        PendingDecision decision = handler.openDecision(human, property, "Buy Brown 1?", () -> callbackTriggered.set(true));

        var result = handler.handle(new BuyPropertyCommand("local-session", "player-" + human.getId(), decision.decisionId(), property.getSpotType().name()));

        assertTrue(result.accepted());
        assertEquals(human, property.getOwnerPlayer());
        assertNull(pendingDecisionRef.get());
        assertTrue(callbackTriggered.get());
    }

    @Test
    void buyPropertyCommandRejectsWrongActor() {
        Player human = new Player("Human", Color.MEDIUMPURPLE, 1500, 1, ComputerPlayerProfile.HUMAN);
        StreetProperty property = new StreetProperty(SpotType.B1);
        var pendingDecisionRef = new AtomicReference<PendingDecision>();
        var auctionStateRef = new AtomicReference<AuctionState>();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, new FakeGateway());

        PendingDecision decision = handler.openDecision(human, property, "Buy Brown 1?", () -> {
        });

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
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, gateway);

        PendingDecision decision = handler.openDecision(human, property, "Buy Brown 1?", () -> {
        });

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
        FakeGateway gateway = new FakeGateway();
        PropertyPurchaseCommandHandler handler = newHandler(human, pendingDecisionRef, auctionStateRef, gateway);

        PendingDecision decision = handler.openDecision(human, property, "Buy Brown 1?", () -> {
        });

        var result = handler.handle(new DeclinePropertyCommand("local-session", "player-" + human.getId(), decision.decisionId(), property.getSpotType().name()));

        assertTrue(result.accepted());
        assertNull(pendingDecisionRef.get());
        assertTrue(gateway.auctionStarted);
        assertNotNull(auctionStateRef.get());
        assertEquals(property.getSpotType().name(), auctionStateRef.get().propertyId());
        assertEquals(AuctionStatus.ACTIVE, auctionStateRef.get().status());
    }

    private PropertyPurchaseCommandHandler newHandler(
            Player human,
            AtomicReference<PendingDecision> pendingDecisionRef,
            AtomicReference<AuctionState> auctionStateRef,
            PropertyPurchaseGateway gateway
    ) {
        return new PropertyPurchaseCommandHandler(
                "local-session",
                () -> new SessionState(
                        "local-session",
                        0L,
                        SessionStatus.IN_PROGRESS,
                        List.of(new SeatState("seat-0", 0, "player-" + human.getId(), SeatKind.HUMAN, ControlMode.MANUAL, human.getName())),
                        List.of(new PlayerSnapshot("player-" + human.getId(), "seat-0", human.getName(), human.getMoneyAmount(), -1, false, false, false, 0, List.of())),
                        new TurnState("player-" + human.getId(), pendingDecisionRef.get() != null ? TurnPhase.WAITING_FOR_DECISION : auctionStateRef.get() != null ? TurnPhase.WAITING_FOR_AUCTION : TurnPhase.WAITING_FOR_ROLL, false, false),
                        pendingDecisionRef.get(),
                        auctionStateRef.get(),
                        null
                ),
                pendingDecisionRef::set,
                auctionStateRef::set,
                gateway
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
