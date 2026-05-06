package fi.monopoly.presentation.legacy.session.projection;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.decision.PendingDecision;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static fi.monopoly.text.UiTexts.text;

/**
 * Projects the current legacy runtime into an authoritative {@link SessionState} snapshot.
 *
 * <p>This adapter exists because the desktop game still runs many behaviors through mutable
 * runtime objects. The projector lets the newer application layer observe that runtime through a
 * stable session model while the remaining legacy responsibilities are being peeled away.</p>
 */
@RequiredArgsConstructor
public final class LegacySessionProjector {
    private final String sessionId;
    private final Supplier<Players> playersSupplier;
    private final Supplier<LegacyPopupSnapshot> popupSupplier;
    private final Supplier<DebtState> debtStateSupplier;
    private final BooleanSupplier pausedSupplier;
    private final BooleanSupplier gameOverSupplier;
    private final Supplier<String> winnerPlayerIdSupplier;
    private final BooleanSupplier canRollSupplier;
    private final BooleanSupplier canEndTurnSupplier;

    public SessionState project() {
        Players players = playersSupplier.get();
        List<Player> playerList = players == null ? List.of() : players.getPlayers();
        Map<Player, PlayerIdentity> identities = mapPlayerIdentities(playerList);
        TurnState turnState = buildTurnState(players);
        return new SessionState(
                sessionId,
                0L,
                buildSessionStatus(),
                buildSeats(playerList, identities),
                buildPlayerSnapshots(playerList, identities),
                buildPropertySnapshots(identities),
                turnState,
                buildPendingDecision(turnState.activePlayerId()),
                null,
                buildActiveDebt(),
                null,
                buildWinnerPlayerId()
        );
    }

    private SessionStatus buildSessionStatus() {
        if (gameOverSupplier.getAsBoolean()) {
            return SessionStatus.GAME_OVER;
        }
        if (pausedSupplier.getAsBoolean()) {
            return SessionStatus.PAUSED;
        }
        return SessionStatus.IN_PROGRESS;
    }

    private TurnState buildTurnState(Players players) {
        String activePlayerId = null;
        if (players != null && players.getTurn() != null) {
            activePlayerId = playerId(players.getTurn());
        }
        return new TurnState(
                activePlayerId,
                resolveTurnPhase(),
                canRollSupplier.getAsBoolean(),
                canEndTurnSupplier.getAsBoolean()
        );
    }

    private TurnPhase resolveTurnPhase() {
        if (gameOverSupplier.getAsBoolean()) {
            return TurnPhase.GAME_OVER;
        }
        if (debtStateSupplier.get() != null) {
            return TurnPhase.RESOLVING_DEBT;
        }
        if (popupSupplier.get() != null) {
            return TurnPhase.WAITING_FOR_DECISION;
        }
        if (canRollSupplier.getAsBoolean()) {
            return TurnPhase.WAITING_FOR_ROLL;
        }
        if (canEndTurnSupplier.getAsBoolean()) {
            return TurnPhase.WAITING_FOR_END_TURN;
        }
        return TurnPhase.UNKNOWN;
    }

    private List<SeatState> buildSeats(List<Player> playerList, Map<Player, PlayerIdentity> identities) {
        List<SeatState> seats = new ArrayList<>(playerList.size());
        for (Player player : playerList) {
            PlayerIdentity identity = identities.get(player);
            seats.add(new SeatState(
                    identity.seatId(),
                    identity.seatIndex(),
                    identity.playerId(),
                    player.isComputerControlled() ? SeatKind.BOT : SeatKind.HUMAN,
                    ControlMode.MANUAL,
                    player.getName(),
                    player.getComputerProfile().name(),
                    toColorHex(player.getColor())
            ));
        }
        return seats;
    }

    private List<PlayerSnapshot> buildPlayerSnapshots(List<Player> playerList, Map<Player, PlayerIdentity> identities) {
        List<PlayerSnapshot> snapshots = new ArrayList<>(playerList.size());
        for (Player player : playerList) {
            PlayerIdentity identity = identities.get(player);
            snapshots.add(new PlayerSnapshot(
                    identity.playerId(),
                    identity.seatId(),
                    player.getName(),
                    player.getMoneyAmount(),
                    player.getSpot() == null ? -1 : player.getSpot().getSpotType().ordinal(),
                    false,
                    false,
                    player.isInJail(),
                    JailSpot.jailTimeLeftMap.getOrDefault(player, 0),
                    player.getGetOutOfJailCardCount(),
                    player.getOwnedProperties().stream().map(property -> property.getSpotType().name()).toList()
            ));
        }
        return snapshots;
    }

    private List<PropertyStateSnapshot> buildPropertySnapshots(Map<Player, PlayerIdentity> identities) {
        List<PropertyStateSnapshot> snapshots = new ArrayList<>();
        for (SpotType spotType : SpotType.values()) {
            if (spotType.streetType == null || spotType.streetType.placeType == null) {
                continue;
            }
            PlaceType placeType = spotType.streetType.placeType;
            if (placeType != PlaceType.STREET && placeType != PlaceType.RAILROAD && placeType != PlaceType.UTILITY) {
                continue;
            }
            Property property = PropertyFactory.getProperty(spotType);
            String ownerPlayerId = null;
            if (property.getOwnerPlayer() != null) {
                PlayerIdentity identity = identities.get(property.getOwnerPlayer());
                ownerPlayerId = identity != null ? identity.playerId() : playerId(property.getOwnerPlayer());
            }
            int houseCount = 0;
            int hotelCount = 0;
            if (property instanceof StreetProperty streetProperty) {
                houseCount = streetProperty.getHouseCount();
                hotelCount = streetProperty.getHotelCount();
            }
            snapshots.add(new PropertyStateSnapshot(
                    spotType.name(),
                    ownerPlayerId,
                    property.isMortgaged(),
                    houseCount,
                    hotelCount
            ));
        }
        return snapshots;
    }

    private PendingDecision buildPendingDecision(String activePlayerId) {
        LegacyPopupSnapshot popup = popupSupplier.get();
        if (popup == null || popup.message() == null || popup.message().isBlank()) {
            return null;
        }
        DecisionType decisionType = mapDecisionType(popup);
        return new PendingDecision(
                popup.kind() + ":" + Objects.hash(activePlayerId, popup.message(), popup.actions()),
                decisionType,
                activePlayerId,
                mapDecisionActions(popup, decisionType),
                popup.message(),
                null
        );
    }

    private DecisionType mapDecisionType(LegacyPopupSnapshot popup) {
        if ("propertyOffer".equals(popup.kind())) {
            return DecisionType.PROPERTY_PURCHASE;
        }
        if (Objects.equals(text("jail.payOrCardPrompt"), popup.message())) {
            return DecisionType.JAIL_CHOICE;
        }
        if (popup.actions().size() > 1) {
            return DecisionType.GENERIC_CONFIRM;
        }
        if (popup.actions().size() == 1) {
            return DecisionType.GENERIC_INFO;
        }
        return DecisionType.UNKNOWN;
    }

    private List<DecisionAction> mapDecisionActions(LegacyPopupSnapshot popup, DecisionType decisionType) {
        if (decisionType == DecisionType.PROPERTY_PURCHASE) {
            return List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY);
        }
        if (popup.actions().isEmpty()) {
            return List.of();
        }
        if (popup.actions().size() == 1) {
            return List.of(DecisionAction.PRIMARY);
        }
        return List.of(DecisionAction.PRIMARY, DecisionAction.SECONDARY);
    }

    private String buildWinnerPlayerId() {
        return winnerPlayerIdSupplier.get();
    }

    private DebtStateModel buildActiveDebt() {
        DebtState debtState = debtStateSupplier.get();
        if (debtState == null || debtState.paymentRequest() == null) {
            return null;
        }
        PaymentRequest request = debtState.paymentRequest();
        List<DebtAction> allowedActions = new ArrayList<>(List.of(
                DebtAction.PAY_DEBT_NOW,
                DebtAction.MORTGAGE_PROPERTY,
                DebtAction.SELL_BUILDING,
                DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET
        ));
        if (debtState.bankruptcyRisk()) {
            allowedActions.add(DebtAction.DECLARE_BANKRUPTCY);
        }
        return new DebtStateModel(
                "debt:" + playerId(request.debtor()) + ":" + request.amount() + ":" + request.reason().hashCode(),
                playerId(request.debtor()),
                request.target() instanceof fi.monopoly.components.payment.PlayerTarget ? DebtCreditorType.PLAYER : DebtCreditorType.BANK,
                request.target() instanceof fi.monopoly.components.payment.PlayerTarget playerTarget ? playerId(playerTarget.player()) : null,
                request.amount(),
                request.reason(),
                debtState.bankruptcyRisk(),
                request.debtor().getMoneyAmount(),
                request.debtor().getTotalLiquidationValue(),
                allowedActions
        );
    }

    private Map<Player, PlayerIdentity> mapPlayerIdentities(List<Player> playerList) {
        Map<Player, PlayerIdentity> identities = new LinkedHashMap<>();
        for (int i = 0; i < playerList.size(); i++) {
            Player player = playerList.get(i);
            identities.put(player, new PlayerIdentity("seat-" + i, playerId(player), i));
        }
        return identities;
    }

    private String playerId(Player player) {
        return player == null ? null : "player-" + player.getId();
    }

    private String toColorHex(javafx.scene.paint.Color color) {
        if (color == null) {
            return "#000000";
        }
        return String.format(
                "#%02X%02X%02X",
                Math.round(color.getRed() * 255),
                Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255)
        );
    }

    private record PlayerIdentity(String seatId, String playerId, int seatIndex) {
    }
}
