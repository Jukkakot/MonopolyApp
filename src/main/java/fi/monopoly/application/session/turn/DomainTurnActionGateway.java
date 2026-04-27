package fi.monopoly.application.session.turn;

import fi.monopoly.application.session.SessionStateStore;
import fi.monopoly.application.session.purchase.PropertyPurchaseFlow;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * Pure domain implementation of {@link TurnActionGateway} — no Processing runtime objects.
 *
 * <p>Handles the full dice-roll-and-move cycle, landing-spot effects, end-turn, building
 * purchases, and mortgage toggling. All changes are persisted via {@link SessionStateStore}.</p>
 *
 * <h2>Landing effects implemented</h2>
 * <ul>
 *   <li>Movement + GO bonus (€200)</li>
 *   <li>GO_TO_JAIL — player is jailed immediately</li>
 *   <li>Unowned property — property purchase decision opened via {@link PropertyPurchaseFlow}</li>
 *   <li>Owned property — rent paid immediately if cash allows; otherwise {@link DebtStateModel}
 *       is opened and turn phase moves to {@code RESOLVING_DEBT}</li>
 *   <li>Mortgaged property — no rent, player continues</li>
 *   <li>Tax spots — direct deduction or debt if insufficient cash</li>
 *   <li>Chance/Community cards — not yet modelled; treated as no-op for now</li>
 *   <li>Doubles — player re-rolls; third consecutive doubles → jail</li>
 *   <li>In-jail turn — doubles escape jail; last forced round deducts €50 fine</li>
 * </ul>
 */
@Slf4j
public final class DomainTurnActionGateway implements TurnActionGateway {

    private static final int BOARD_SIZE = SpotType.SPOT_TYPES.size(); // 40
    private static final int JAIL_INDEX = SpotType.SPOT_TYPES.indexOf(SpotType.JAIL); // 10
    private static final int MAX_CONSECUTIVE_DOUBLES = 3;
    private static final int GO_REWARD = 200;
    private static final int GET_OUT_OF_JAIL_FEE = 50;
    private static final int INITIAL_JAIL_ROUNDS = 3;

    private final SessionStateStore store;
    private final PropertyPurchaseFlow propertyPurchaseFlow;
    private final IntSupplier singleDieSupplier;

    public DomainTurnActionGateway(SessionStateStore store, PropertyPurchaseFlow propertyPurchaseFlow) {
        this(store, propertyPurchaseFlow, () -> 1 + ThreadLocalRandom.current().nextInt(6));
    }

    /** Package-private constructor for testing with controlled dice. */
    DomainTurnActionGateway(SessionStateStore store, PropertyPurchaseFlow propertyPurchaseFlow, IntSupplier singleDieSupplier) {
        this.store = store;
        this.propertyPurchaseFlow = propertyPurchaseFlow;
        this.singleDieSupplier = singleDieSupplier;
    }

    // -------------------------------------------------------------------------
    // TurnActionGateway
    // -------------------------------------------------------------------------

    @Override
    public boolean rollDice() {
        SessionState state = store.get();
        String activePlayerId = state.turn().activePlayerId();
        if (activePlayerId == null) return false;

        PlayerSnapshot activePlayer = findPlayer(state, activePlayerId);
        if (activePlayer == null) return false;

        int die1 = singleDieSupplier.getAsInt();
        int die2 = singleDieSupplier.getAsInt();
        int total = die1 + die2;
        boolean isDoubles = die1 == die2;
        int newConsecutive = isDoubles ? state.turn().consecutiveDoubles() + 1 : 0;

        log.debug("rollDice player={} die1={} die2={} total={} doubles={} consecutive={}",
                activePlayerId, die1, die2, total, isDoubles, newConsecutive);

        // Third consecutive doubles → jail without collecting GO
        if (newConsecutive >= MAX_CONSECUTIVE_DOUBLES) {
            log.info("Player {} sent to jail for {} consecutive doubles", activePlayerId, newConsecutive);
            store.update(s -> applyGoToJail(s, activePlayerId));
            return true;
        }

        // In-jail: special handling
        if (activePlayer.inJail()) {
            handleInJailRoll(activePlayer, total, isDoubles);
            return true;
        }

        // Normal movement
        int rawNew = activePlayer.boardIndex() + total;
        int newIndex = rawNew % BOARD_SIZE;
        boolean passedGo = rawNew >= BOARD_SIZE;
        SpotType landedSpot = SpotType.SPOT_TYPES.get(newIndex);

        if (landedSpot == SpotType.GO_TO_JAIL) {
            log.info("Player {} landed on Go To Jail", activePlayerId);
            // No GO reward even if passed GO on the way to GO_TO_JAIL (standard rules)
            store.update(s -> applyGoToJail(s, activePlayerId));
            return true;
        }

        // Move player + apply GO bonus
        final int goBonus = passedGo ? GO_REWARD : 0;
        store.update(s -> s.toBuilder()
                .players(updatePosition(s.players(), activePlayerId, newIndex, goBonus))
                .turn(withConsecutive(s.turn(), newConsecutive))
                .build());

        applyLandingEffects(activePlayerId, landedSpot, isDoubles, newConsecutive, total);
        return true;
    }

    @Override
    public boolean endTurn() {
        store.update(state -> {
            String next = DomainTurnContinuationGateway.nextActivePlayerId(state, state.turn().activePlayerId());
            if (next == null) return state;
            return state.toBuilder()
                    .turn(new TurnState(next, TurnPhase.WAITING_FOR_ROLL, true, false, 0))
                    .build();
        });
        return true;
    }

    @Override
    public boolean buyBuildingRound(String propertyId) {
        SpotType spotType;
        try {
            spotType = SpotType.valueOf(propertyId);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (spotType.streetType.placeType != PlaceType.STREET) return false;

        store.update(state -> {
            String activePlayerId = state.turn().activePlayerId();
            if (activePlayerId == null) return state;

            List<SpotType> colorSet = spotsOfType(spotType.streetType);

            boolean ownsAll = colorSet.stream()
                    .allMatch(s -> activePlayerId.equals(ownerOf(state, s.name())));
            if (!ownsAll) return state;

            PropertyStateSnapshot targetProp = findProperty(state, propertyId);
            if (targetProp == null || targetProp.hotelCount() > 0) return state;

            // Even building rule: must not be more than 1 house ahead of the set minimum
            int targetLevel = targetProp.houseCount();
            int minLevelInSet = colorSet.stream()
                    .mapToInt(s -> {
                        PropertyStateSnapshot p = findProperty(state, s.name());
                        if (p == null) return 0;
                        return p.hotelCount() > 0 ? 5 : p.houseCount();
                    })
                    .min().orElse(0);
            if (targetLevel > minLevelInSet) return state;

            int housePrice = spotType.getIntegerProperty("housePrice");
            if (housePrice <= 0) return state;

            PlayerSnapshot player = findPlayer(state, activePlayerId);
            if (player == null || player.cash() < housePrice) return state;

            int newHouseCount = targetProp.houseCount() + 1;
            boolean becomesHotel = newHouseCount >= 5;

            List<PropertyStateSnapshot> updatedProps = state.properties().stream()
                    .map(p -> p.propertyId().equals(propertyId)
                            ? new PropertyStateSnapshot(p.propertyId(), p.ownerPlayerId(), p.mortgaged(),
                                    becomesHotel ? 0 : newHouseCount, becomesHotel ? 1 : 0)
                            : p)
                    .toList();
            List<PlayerSnapshot> updatedPlayers = updateCash(state.players(), activePlayerId, -housePrice);
            return state.toBuilder().properties(updatedProps).players(updatedPlayers).build();
        });
        return true;
    }

    @Override
    public boolean toggleMortgage(String propertyId) {
        try {
            SpotType.valueOf(propertyId);
        } catch (IllegalArgumentException e) {
            return false;
        }

        store.update(state -> {
            String activePlayerId = state.turn().activePlayerId();
            if (activePlayerId == null) return state;

            PropertyStateSnapshot property = findProperty(state, propertyId);
            if (property == null || !activePlayerId.equals(property.ownerPlayerId())) return state;

            int mortgageValue = SpotType.valueOf(propertyId).getIntegerProperty("price") / 2;

            if (property.mortgaged()) {
                int unmortgageCost = mortgageValue + (int) (mortgageValue * 0.1);
                PlayerSnapshot player = findPlayer(state, activePlayerId);
                if (player == null || player.cash() < unmortgageCost) return state;
                return state.toBuilder()
                        .properties(replaceProperty(state.properties(), propertyId,
                                new PropertyStateSnapshot(property.propertyId(), property.ownerPlayerId(),
                                        false, property.houseCount(), property.hotelCount())))
                        .players(updateCash(state.players(), activePlayerId, -unmortgageCost))
                        .build();
            } else {
                return state.toBuilder()
                        .properties(replaceProperty(state.properties(), propertyId,
                                new PropertyStateSnapshot(property.propertyId(), property.ownerPlayerId(),
                                        true, property.houseCount(), property.hotelCount())))
                        .players(updateCash(state.players(), activePlayerId, mortgageValue))
                        .build();
            }
        });
        return true;
    }

    // -------------------------------------------------------------------------
    // Private helpers: jail
    // -------------------------------------------------------------------------

    private void handleInJailRoll(PlayerSnapshot activePlayer, int total, boolean isDoubles) {
        String playerId = activePlayer.playerId();
        int roundsLeft = activePlayer.jailRoundsRemaining();

        if (isDoubles) {
            log.info("Player {} rolled doubles and escapes jail", playerId);
            int rawNew = JAIL_INDEX + total;
            int newIndex = rawNew % BOARD_SIZE;
            boolean passedGo = rawNew >= BOARD_SIZE;
            SpotType landedSpot = SpotType.SPOT_TYPES.get(newIndex);
            final int goBonus = passedGo ? GO_REWARD : 0;

            store.update(s -> s.toBuilder()
                    .players(updateJailRelease(s.players(), playerId, newIndex, goBonus))
                    .turn(withConsecutive(s.turn(), 0))
                    .build());

            if (landedSpot == SpotType.GO_TO_JAIL) {
                store.update(s -> applyGoToJail(s, playerId));
            } else {
                // No re-roll bonus after jail escape even if doubles
                applyLandingEffects(playerId, landedSpot, false, 0, total);
            }
        } else if (roundsLeft <= 1) {
            log.info("Player {} must pay jail fine (last round), total={}", playerId, total);
            int rawNew = JAIL_INDEX + total;
            int newIndex = rawNew % BOARD_SIZE;
            boolean passedGo = rawNew >= BOARD_SIZE;
            SpotType landedSpot = SpotType.SPOT_TYPES.get(newIndex);
            final int goBonus = passedGo ? GO_REWARD : 0;

            store.update(s -> s.toBuilder()
                    .players(updateJailFineAndRelease(s.players(), playerId, newIndex, goBonus))
                    .turn(withConsecutive(s.turn(), 0))
                    .build());

            if (landedSpot == SpotType.GO_TO_JAIL) {
                store.update(s -> applyGoToJail(s, playerId));
            } else {
                applyLandingEffects(playerId, landedSpot, false, 0, total);
            }
        } else {
            log.debug("Player {} stays in jail, roundsLeft → {}", playerId, roundsLeft - 1);
            store.update(s -> {
                List<PlayerSnapshot> updated = s.players().stream()
                        .map(p -> playerId.equals(p.playerId())
                                ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash(),
                                        p.boardIndex(), p.bankrupt(), p.eliminated(), true,
                                        roundsLeft - 1, p.getOutOfJailCards(), p.ownedPropertyIds())
                                : p)
                        .toList();
                return s.toBuilder()
                        .players(updated)
                        .turn(new TurnState(s.turn().activePlayerId(), TurnPhase.WAITING_FOR_END_TURN, false, true, 0))
                        .build();
            });
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers: landing effects
    // -------------------------------------------------------------------------

    private void applyLandingEffects(String playerId, SpotType landedSpot, boolean isDoubles, int consecutiveDoubles, int diceTotal) {
        PlaceType placeType = landedSpot.streetType.placeType;

        if (placeType == PlaceType.STREET || placeType == PlaceType.RAILROAD || placeType == PlaceType.UTILITY) {
            SessionState current = store.get();
            PropertyStateSnapshot property = findProperty(current, landedSpot.name());
            String ownerId = property != null ? property.ownerPlayerId() : null;

            if (ownerId == null) {
                openPropertyDecision(current, playerId, landedSpot, isDoubles, consecutiveDoubles);
                return;
            }
            if (ownerId.equals(playerId) || property.mortgaged()) {
                store.update(s -> s.toBuilder().turn(postMovePhase(s.turn().activePlayerId(), isDoubles, consecutiveDoubles)).build());
                return;
            }
            int rent = calculateRent(current, landedSpot, property, ownerId, diceTotal);
            applyRentOrDebt(current, playerId, ownerId, rent, isDoubles, consecutiveDoubles,
                    landedSpot.getStringProperty("name"));
            return;
        }

        if (placeType == PlaceType.TAX) {
            SessionState current = store.get();
            int tax = landedSpot.getIntegerProperty("price");
            applyRentOrDebt(current, playerId, null, tax, isDoubles, consecutiveDoubles, "Tax");
            return;
        }

        // CORNER (GO / JAIL visiting / FREE_PARKING) and PICK_CARD → advance turn
        store.update(s -> s.toBuilder().turn(postMovePhase(s.turn().activePlayerId(), isDoubles, consecutiveDoubles)).build());
    }

    private void openPropertyDecision(SessionState state, String playerId, SpotType spot,
                                       boolean isDoubles, int consecutiveDoubles) {
        int price = spot.getIntegerProperty("price");
        String displayName = spot.getStringProperty("name");
        TurnContinuationAction completionAction = isDoubles
                ? TurnContinuationAction.APPLY_TURN_FOLLOW_UP
                : TurnContinuationAction.END_TURN_WITH_SWITCH;
        TurnContinuationState continuation = new TurnContinuationState(
                "cont:" + playerId + ":purchase:" + spot.name(),
                playerId,
                TurnContinuationType.RESUME_AFTER_AUCTION,
                completionAction,
                spot.name(), null
        );
        store.update(s -> s.toBuilder()
                .turn(new TurnState(s.turn().activePlayerId(), TurnPhase.WAITING_FOR_DECISION, false, false, consecutiveDoubles))
                .build());
        propertyPurchaseFlow.begin(playerId, spot.name(), displayName, price,
                displayName + " €" + price, continuation);
    }

    private void applyRentOrDebt(SessionState state, String debtorId, String creditorId, int amount,
                                  boolean isDoubles, int consecutiveDoubles, String reason) {
        if (amount <= 0) {
            store.update(s -> s.toBuilder().turn(postMovePhase(s.turn().activePlayerId(), isDoubles, consecutiveDoubles)).build());
            return;
        }
        PlayerSnapshot debtor = findPlayer(state, debtorId);
        if (debtor == null) return;

        if (debtor.cash() >= amount) {
            store.update(s -> {
                List<PlayerSnapshot> updated = s.players().stream()
                        .map(p -> {
                            if (debtorId.equals(p.playerId())) {
                                return new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash() - amount,
                                        p.boardIndex(), p.bankrupt(), p.eliminated(), p.inJail(),
                                        p.jailRoundsRemaining(), p.getOutOfJailCards(), p.ownedPropertyIds());
                            }
                            if (creditorId != null && creditorId.equals(p.playerId())) {
                                return new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash() + amount,
                                        p.boardIndex(), p.bankrupt(), p.eliminated(), p.inJail(),
                                        p.jailRoundsRemaining(), p.getOutOfJailCards(), p.ownedPropertyIds());
                            }
                            return p;
                        })
                        .toList();
                return s.toBuilder()
                        .players(updated)
                        .turn(postMovePhase(s.turn().activePlayerId(), isDoubles, consecutiveDoubles))
                        .build();
            });
        } else {
            openDebt(state, debtorId, creditorId, amount, isDoubles, consecutiveDoubles, reason);
        }
    }

    private void openDebt(SessionState state, String debtorId, String creditorId, int amount,
                           boolean isDoubles, int consecutiveDoubles, String reason) {
        int estimatedLiquidation = estimateLiquidation(state, debtorId);
        PlayerSnapshot debtor = findPlayer(state, debtorId);
        int currentCash = debtor != null ? debtor.cash() : 0;
        boolean bankruptcyRisk = currentCash + estimatedLiquidation < amount;

        List<DebtAction> actions = new ArrayList<>(List.of(
                DebtAction.PAY_DEBT_NOW, DebtAction.MORTGAGE_PROPERTY,
                DebtAction.SELL_BUILDING, DebtAction.SELL_BUILDING_ROUNDS_ACROSS_SET));
        if (bankruptcyRisk) actions.add(DebtAction.DECLARE_BANKRUPTCY);

        DebtStateModel debt = new DebtStateModel(
                "debt:" + debtorId + ":" + System.currentTimeMillis(),
                debtorId,
                creditorId != null ? DebtCreditorType.PLAYER : DebtCreditorType.BANK,
                creditorId,
                amount, reason, bankruptcyRisk, currentCash, estimatedLiquidation, actions
        );
        TurnContinuationState continuation = new TurnContinuationState(
                "cont:" + debtorId + ":afterDebt",
                debtorId,
                TurnContinuationType.RESUME_AFTER_DEBT,
                isDoubles ? TurnContinuationAction.APPLY_TURN_FOLLOW_UP : TurnContinuationAction.END_TURN_WITH_SWITCH,
                null, reason
        );
        store.update(s -> s.toBuilder()
                .activeDebt(debt)
                .turnContinuationState(continuation)
                .turn(new TurnState(s.turn().activePlayerId(), TurnPhase.RESOLVING_DEBT, false, false, consecutiveDoubles))
                .build());
    }

    // -------------------------------------------------------------------------
    // Private helpers: state transitions
    // -------------------------------------------------------------------------

    private static SessionState applyGoToJail(SessionState state, String playerId) {
        List<PlayerSnapshot> updated = state.players().stream()
                .map(p -> playerId.equals(p.playerId())
                        ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash(),
                                JAIL_INDEX, p.bankrupt(), p.eliminated(), true, INITIAL_JAIL_ROUNDS,
                                p.getOutOfJailCards(), p.ownedPropertyIds())
                        : p)
                .toList();
        return state.toBuilder()
                .players(updated)
                .turn(new TurnState(state.turn().activePlayerId(), TurnPhase.WAITING_FOR_END_TURN, false, true, 0))
                .build();
    }

    private static TurnState postMovePhase(String activePlayerId, boolean isDoubles, int consecutiveDoubles) {
        if (isDoubles) {
            return new TurnState(activePlayerId, TurnPhase.WAITING_FOR_ROLL, true, false, consecutiveDoubles);
        }
        return new TurnState(activePlayerId, TurnPhase.WAITING_FOR_END_TURN, false, true, 0);
    }

    private static TurnState withConsecutive(TurnState current, int consecutiveDoubles) {
        return new TurnState(current.activePlayerId(), current.phase(), current.canRoll(), current.canEndTurn(), consecutiveDoubles);
    }

    // -------------------------------------------------------------------------
    // Private helpers: player list mutation
    // -------------------------------------------------------------------------

    private static List<PlayerSnapshot> updatePosition(List<PlayerSnapshot> players, String playerId, int newIndex, int cashDelta) {
        return players.stream()
                .map(p -> playerId.equals(p.playerId())
                        ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash() + cashDelta,
                                newIndex, p.bankrupt(), p.eliminated(), p.inJail(),
                                p.jailRoundsRemaining(), p.getOutOfJailCards(), p.ownedPropertyIds())
                        : p)
                .toList();
    }

    private static List<PlayerSnapshot> updateJailRelease(List<PlayerSnapshot> players, String playerId, int newIndex, int cashDelta) {
        return players.stream()
                .map(p -> playerId.equals(p.playerId())
                        ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash() + cashDelta,
                                newIndex, p.bankrupt(), p.eliminated(), false, 0,
                                p.getOutOfJailCards(), p.ownedPropertyIds())
                        : p)
                .toList();
    }

    private static List<PlayerSnapshot> updateJailFineAndRelease(List<PlayerSnapshot> players, String playerId, int newIndex, int cashDelta) {
        return players.stream()
                .map(p -> playerId.equals(p.playerId())
                        ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(),
                                Math.max(0, p.cash() - GET_OUT_OF_JAIL_FEE) + cashDelta,
                                newIndex, p.bankrupt(), p.eliminated(), false, 0,
                                p.getOutOfJailCards(), p.ownedPropertyIds())
                        : p)
                .toList();
    }

    static List<PlayerSnapshot> updateCash(List<PlayerSnapshot> players, String playerId, int delta) {
        return players.stream()
                .map(p -> playerId.equals(p.playerId())
                        ? new PlayerSnapshot(p.playerId(), p.seatId(), p.name(), p.cash() + delta,
                                p.boardIndex(), p.bankrupt(), p.eliminated(), p.inJail(),
                                p.jailRoundsRemaining(), p.getOutOfJailCards(), p.ownedPropertyIds())
                        : p)
                .toList();
    }

    static List<PropertyStateSnapshot> replaceProperty(List<PropertyStateSnapshot> properties, String propertyId,
                                                        PropertyStateSnapshot replacement) {
        return properties.stream()
                .map(p -> p.propertyId().equals(propertyId) ? replacement : p)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Private helpers: rent calculation
    // -------------------------------------------------------------------------

    private static int calculateRent(SessionState state, SpotType landedSpot, PropertyStateSnapshot property,
                                      String ownerId, int diceTotal) {
        PlaceType placeType = landedSpot.streetType.placeType;

        if (placeType == PlaceType.STREET) {
            int[] rents = parseRents(landedSpot.getStringProperty("rents"));
            if (rents.length < 6) return 0;
            if (property.hotelCount() > 0) return rents[5];
            if (property.houseCount() > 0) return rents[Math.min(property.houseCount(), 4)];
            boolean monopoly = spotsOfType(landedSpot.streetType).stream()
                    .allMatch(s -> ownerId.equals(ownerOf(state, s.name())));
            return monopoly ? rents[0] * 2 : rents[0];
        }

        if (placeType == PlaceType.RAILROAD) {
            int[] rents = parseRents(landedSpot.getStringProperty("rents"));
            long owned = spotsOfType(landedSpot.streetType).stream()
                    .filter(s -> ownerId.equals(ownerOf(state, s.name())))
                    .count();
            int idx = (int) Math.min(owned, rents.length - 1);
            return idx > 0 ? rents[idx] : 0;
        }

        if (placeType == PlaceType.UTILITY) {
            long owned = spotsOfType(landedSpot.streetType).stream()
                    .filter(s -> ownerId.equals(ownerOf(state, s.name())))
                    .count();
            int multiplier = owned >= 2 ? 10 : 4;
            return diceTotal * multiplier;
        }

        return 0;
    }

    private static int estimateLiquidation(SessionState state, String playerId) {
        return state.properties().stream()
                .filter(p -> playerId.equals(p.ownerPlayerId()) && !p.mortgaged())
                .mapToInt(p -> {
                    try {
                        return SpotType.valueOf(p.propertyId()).getIntegerProperty("price") / 2;
                    } catch (IllegalArgumentException e) {
                        return 0;
                    }
                })
                .sum();
    }

    private static int[] parseRents(String rentsStr) {
        if (rentsStr == null || rentsStr.isBlank()) return new int[0];
        String[] parts = rentsStr.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    private static List<SpotType> spotsOfType(StreetType streetType) {
        return SpotType.SPOT_TYPES.stream()
                .filter(s -> s.streetType == streetType && s.isProperty)
                .toList();
    }

    private static String ownerOf(SessionState state, String propertyId) {
        return state.properties().stream()
                .filter(p -> propertyId.equals(p.propertyId()))
                .map(PropertyStateSnapshot::ownerPlayerId)
                .findFirst().orElse(null);
    }

    private static PlayerSnapshot findPlayer(SessionState state, String playerId) {
        return state.players().stream()
                .filter(p -> playerId.equals(p.playerId()))
                .findFirst().orElse(null);
    }

    private static PropertyStateSnapshot findProperty(SessionState state, String propertyId) {
        return state.properties().stream()
                .filter(p -> propertyId.equals(p.propertyId()))
                .findFirst().orElse(null);
    }
}
