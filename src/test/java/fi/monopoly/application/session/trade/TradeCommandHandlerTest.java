package fi.monopoly.application.session.trade;

import fi.monopoly.application.command.*;
import fi.monopoly.components.Player;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.trade.BotTradeProfile;
import fi.monopoly.components.trade.TradeDecision;
import fi.monopoly.components.trade.TradeOffer;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TradeCommandHandlerTest {

    @Test
    void openTradeCreatesAuthoritativeTradeState() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        AtomicReference<TradeState> tradeRef = new AtomicReference<>();
        TradeCommandHandler handler = newHandler(proposer, recipient, tradeRef, new FakeTradeGateway(proposer, recipient));

        var result = handler.handle(new OpenTradeCommand("local-session", playerId(proposer), playerId(recipient)));

        assertTrue(result.accepted());
        assertNotNull(tradeRef.get());
        assertEquals(TradeStatus.EDITING, tradeRef.get().status());
        assertEquals(playerId(proposer), tradeRef.get().editingPlayerId());
    }

    @Test
    void editTradeUpdatesCurrentOffer() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        TestObjectFactory.giveProperty(proposer, b1);
        AtomicReference<TradeState> tradeRef = new AtomicReference<>();
        FakeTradeGateway gateway = new FakeTradeGateway(proposer, recipient);
        gateway.registerProperty(b1);
        TradeCommandHandler handler = newHandler(proposer, recipient, tradeRef, gateway);
        handler.handle(new OpenTradeCommand("local-session", playerId(proposer), playerId(recipient)));

        var result = handler.handle(new EditTradeOfferCommand(
                "local-session",
                playerId(proposer),
                tradeRef.get().tradeId(),
                new TradeEditPatch(null, true, 120, List.of(b1.getSpotType().name()), List.of(), true)
        ));

        assertTrue(result.accepted());
        assertEquals(120, tradeRef.get().currentOffer().offeredToRecipient().moneyAmount());
        assertEquals(List.of(b1.getSpotType().name()), tradeRef.get().currentOffer().offeredToRecipient().propertyIds());
        assertEquals(1, tradeRef.get().currentOffer().offeredToRecipient().jailCardCount());
    }

    @Test
    void submitRejectsEmptyTrade() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        AtomicReference<TradeState> tradeRef = new AtomicReference<>();
        TradeCommandHandler handler = newHandler(proposer, recipient, tradeRef, new FakeTradeGateway(proposer, recipient));
        handler.handle(new OpenTradeCommand("local-session", playerId(proposer), playerId(recipient)));

        var result = handler.handle(new SubmitTradeOfferCommand("local-session", playerId(proposer), tradeRef.get().tradeId()));

        assertFalse(result.accepted());
        assertEquals("EMPTY_TRADE", result.rejections().get(0).code());
    }

    @Test
    void acceptAppliesTradeAndClearsState() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        StreetProperty b1 = new StreetProperty(SpotType.B1);
        TestObjectFactory.giveProperty(proposer, b1);
        AtomicReference<TradeState> tradeRef = new AtomicReference<>();
        FakeTradeGateway gateway = new FakeTradeGateway(proposer, recipient);
        gateway.registerProperty(b1);
        TradeCommandHandler handler = newHandler(proposer, recipient, tradeRef, gateway);
        handler.handle(new OpenTradeCommand("local-session", playerId(proposer), playerId(recipient)));
        handler.handle(new EditTradeOfferCommand(
                "local-session",
                playerId(proposer),
                tradeRef.get().tradeId(),
                new TradeEditPatch(null, true, 0, List.of(b1.getSpotType().name()), List.of(), null)
        ));
        handler.handle(new SubmitTradeOfferCommand("local-session", playerId(proposer), tradeRef.get().tradeId()));

        var result = handler.handle(new AcceptTradeCommand("local-session", playerId(recipient), tradeRef.get().tradeId()));

        assertTrue(result.accepted());
        assertNull(tradeRef.get());
        assertTrue(gateway.applyCalled);
        assertEquals(recipient, b1.getOwnerPlayer());
    }

    @Test
    void counterKeepsTradeActiveAndFlipsDecisionActor() {
        Player proposer = new Player("P1", Color.BLACK, 500, 1);
        Player recipient = new Player("P2", Color.BLUE, 400, 2);
        AtomicReference<TradeState> tradeRef = new AtomicReference<>();
        TradeCommandHandler handler = newHandler(proposer, recipient, tradeRef, new FakeTradeGateway(proposer, recipient));
        handler.handle(new OpenTradeCommand("local-session", playerId(proposer), playerId(recipient)));
        handler.handle(new EditTradeOfferCommand(
                "local-session",
                playerId(proposer),
                tradeRef.get().tradeId(),
                new TradeEditPatch(null, true, 50, List.of(), List.of(), null)
        ));
        handler.handle(new SubmitTradeOfferCommand("local-session", playerId(proposer), tradeRef.get().tradeId()));
        handler.handle(new EditTradeOfferCommand(
                "local-session",
                playerId(recipient),
                tradeRef.get().tradeId(),
                new TradeEditPatch(true, true, 80, List.of(), List.of(), null)
        ));

        var result = handler.handle(new CounterTradeCommand("local-session", playerId(recipient), tradeRef.get().tradeId()));

        assertTrue(result.accepted());
        assertNotNull(tradeRef.get());
        assertEquals(TradeStatus.COUNTERED, tradeRef.get().status());
        assertEquals(playerId(proposer), tradeRef.get().decisionRequiredFromPlayerId());
        assertEquals(playerId(recipient), tradeRef.get().currentOffer().proposerPlayerId());
    }

    private TradeCommandHandler newHandler(
            Player proposer,
            Player recipient,
            AtomicReference<TradeState> tradeRef,
            FakeTradeGateway gateway
    ) {
        return new TradeCommandHandler(
                "local-session",
                () -> new SessionState(
                        "local-session",
                        0L,
                        SessionStatus.IN_PROGRESS,
                        List.of(
                                new SeatState("seat-0", 0, playerId(proposer), SeatKind.HUMAN, ControlMode.MANUAL, proposer.getName(), "HUMAN", "#000000"),
                                new SeatState("seat-1", 1, playerId(recipient), SeatKind.HUMAN, ControlMode.MANUAL, recipient.getName(), "HUMAN", "#000000")
                        ),
                        List.of(
                                new PlayerSnapshot(playerId(proposer), "seat-0", proposer.getName(), proposer.getMoneyAmount(), -1, false, false, false, 0, 0, List.of()),
                                new PlayerSnapshot(playerId(recipient), "seat-1", recipient.getName(), recipient.getMoneyAmount(), -1, false, false, false, 0, 0, List.of())
                        ),
                        List.of(),
                        new TurnState(playerId(proposer), TurnPhase.WAITING_FOR_DECISION, false, false),
                        null,
                        null,
                        null,
                        tradeRef.get(),
                        null
                ),
                tradeRef::set,
                gateway
        );
    }

    private static String playerId(Player player) {
        return "player-" + player.getId();
    }

    private static final class FakeTradeGateway implements TradeGateway {
        private boolean applyCalled;
        private final Player proposer;
        private final Player recipient;
        private final Map<String, Property> propertiesById = new HashMap<>();

        private FakeTradeGateway(Player proposer, Player recipient) {
            this.proposer = proposer;
            this.recipient = recipient;
        }

        private void registerProperty(Property property) {
            propertiesById.put(property.getSpotType().name(), property);
        }

        @Override
        public boolean playerExists(String playerId) {
            return playerById(playerId) != null;
        }

        private Player playerById(String playerId) {
            if (playerId(playerId, proposer)) {
                return proposer;
            }
            if (playerId(playerId, recipient)) {
                return recipient;
            }
            return null;
        }

        public TradeOffer toLegacyOffer(TradeOfferState offerState) {
            Player mappedProposer = playerById(offerState.proposerPlayerId());
            Player mappedRecipient = playerById(offerState.recipientPlayerId());
            if (mappedProposer == null || mappedRecipient == null) {
                return null;
            }
            return new TradeOffer(
                    mappedProposer,
                    mappedRecipient,
                    new fi.monopoly.components.trade.TradeSelection(
                            offerState.offeredToRecipient().moneyAmount(),
                            offerState.offeredToRecipient().propertyIds().stream().map(propertiesById::get).filter(java.util.Objects::nonNull).toList(),
                            offerState.offeredToRecipient().jailCardCount() > 0
                    ),
                    new fi.monopoly.components.trade.TradeSelection(
                            offerState.requestedFromRecipient().moneyAmount(),
                            offerState.requestedFromRecipient().propertyIds().stream().map(propertiesById::get).filter(java.util.Objects::nonNull).toList(),
                            offerState.requestedFromRecipient().jailCardCount() > 0
                    )
            );
        }

        @Override
        public boolean isValidOffer(TradeOfferState offerState) {
            TradeOffer offer = toLegacyOffer(offerState);
            return offer != null && offer.isValid();
        }

        @Override
        public boolean applyOffer(TradeOfferState offerState) {
            applyCalled = true;
            TradeOffer offer = toLegacyOffer(offerState);
            return offer != null && offer.apply();
        }

        @Override
        public TradeDecision evaluateForRecipient(TradeOfferState offerState, BotTradeProfile profile, fi.monopoly.components.computer.StrongBotConfig strongConfig) {
            return new TradeDecision(true, 1.0, "ok");
        }

        @Override
        public TradeOfferState proposeCounterOffer(TradeOfferState offerState, BotTradeProfile profile, fi.monopoly.components.computer.StrongBotConfig strongConfig) {
            TradeOffer offer = toLegacyOffer(offerState);
            return offer == null ? null : toState(offer.reversePerspective().withOfferedToRecipient(offer.reversePerspective().offeredToRecipient().withMoneyAmount(80)));
        }

        private TradeOfferState toState(TradeOffer offer) {
            return new TradeOfferState(
                    TradeCommandHandlerTest.playerId(offer.proposer()),
                    TradeCommandHandlerTest.playerId(offer.recipient()),
                    new TradeSelectionState(
                            offer.offeredToRecipient().moneyAmount(),
                            offer.offeredToRecipient().properties().stream().map(property -> property.getSpotType().name()).toList(),
                            offer.offeredToRecipient().jailCard() ? 1 : 0
                    ),
                    new TradeSelectionState(
                            offer.requestedFromRecipient().moneyAmount(),
                            offer.requestedFromRecipient().properties().stream().map(property -> property.getSpotType().name()).toList(),
                            offer.requestedFromRecipient().jailCard() ? 1 : 0
                    )
            );
        }

        private boolean playerId(String playerId, Player player) {
            return TradeCommandHandlerTest.playerId(player).equals(playerId);
        }
    }
}
