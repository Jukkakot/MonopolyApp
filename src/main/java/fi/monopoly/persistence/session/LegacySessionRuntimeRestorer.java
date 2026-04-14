package fi.monopoly.persistence.session;

import fi.monopoly.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.PropertyStateSnapshot;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SeatState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.types.SpotType;
import javafx.scene.paint.Color;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegacySessionRuntimeRestorer {
    public RestoredLegacySessionRuntime restore(MonopolyRuntime runtime, SessionState sessionState) {
        Board board = new Board(runtime);
        Players players = new Players(runtime);
        Map<String, PlayerSnapshot> playerSnapshots = indexPlayerSnapshots(sessionState.players());
        Map<String, Player> playersById = restorePlayers(runtime, board, players, sessionState.seats(), playerSnapshots);
        restoreProperties(sessionState.properties(), playersById);
        restoreJailState(sessionState.players(), playersById);
        restoreTurn(sessionState, playersById, players);
        return new RestoredLegacySessionRuntime(board, players, Map.copyOf(playersById));
    }

    private Map<String, PlayerSnapshot> indexPlayerSnapshots(List<PlayerSnapshot> snapshots) {
        Map<String, PlayerSnapshot> byId = new LinkedHashMap<>();
        for (PlayerSnapshot snapshot : snapshots) {
            byId.put(snapshot.playerId(), snapshot);
        }
        return byId;
    }

    private Map<String, Player> restorePlayers(
            MonopolyRuntime runtime,
            Board board,
            Players players,
            List<SeatState> seats,
            Map<String, PlayerSnapshot> playerSnapshots
    ) {
        PropertyFactory.resetState();
        JailSpot.jailTimeLeftMap.clear();
        Map<String, Player> playersById = new LinkedHashMap<>();
        for (SeatState seat : seats.stream().sorted(java.util.Comparator.comparingInt(SeatState::seatIndex)).toList()) {
            PlayerSnapshot snapshot = playerSnapshots.get(seat.playerId());
            if (snapshot == null) {
                throw new IllegalArgumentException("Missing player snapshot for seat " + seat.seatId());
            }
            Spot spot = resolveSpot(board, snapshot.boardIndex());
            Player player = Player.restore(
                    runtime,
                    snapshot.name(),
                    resolveColor(seat),
                    spot,
                    resolveProfile(seat),
                    parsePlayerId(snapshot.playerId(), seat.seatIndex()),
                    snapshot.cash(),
                    seat.seatIndex() + 1,
                    snapshot.getOutOfJailCards()
            );
            players.addPlayer(player);
            playersById.put(snapshot.playerId(), player);
        }
        return playersById;
    }

    private void restoreProperties(List<PropertyStateSnapshot> propertySnapshots, Map<String, Player> playersById) {
        PropertyFactory.resetState();
        for (PropertyStateSnapshot snapshot : propertySnapshots) {
            Property property = PropertyFactory.getProperty(SpotType.valueOf(snapshot.propertyId()));
            Player owner = snapshot.ownerPlayerId() == null ? null : playersById.get(snapshot.ownerPlayerId());
            property.restoreState(owner, snapshot.mortgaged());
            if (owner != null) {
                owner.addOwnedProperty(property);
            }
            if (property instanceof StreetProperty streetProperty) {
                streetProperty.restoreBuildings(snapshot.houseCount(), snapshot.hotelCount());
            }
        }
    }

    private void restoreJailState(List<PlayerSnapshot> playerSnapshots, Map<String, Player> playersById) {
        for (PlayerSnapshot snapshot : playerSnapshots) {
            if (!snapshot.inJail() || snapshot.jailRoundsRemaining() <= 0) {
                continue;
            }
            Player player = playersById.get(snapshot.playerId());
            if (player == null) {
                continue;
            }
            JailSpot.jailTimeLeftMap.put(player, snapshot.jailRoundsRemaining());
            player.setCoords(player.getSpot().getTokenCoords(player));
        }
    }

    private void restoreTurn(SessionState sessionState, Map<String, Player> playersById, Players players) {
        if (sessionState.turn() == null || sessionState.turn().activePlayerId() == null) {
            return;
        }
        Player activePlayer = playersById.get(sessionState.turn().activePlayerId());
        if (activePlayer != null) {
            players.restoreTurn(activePlayer);
        }
    }

    private Spot resolveSpot(Board board, int boardIndex) {
        if (boardIndex < 0 || boardIndex >= SpotType.values().length) {
            return board.getSpots().get(0);
        }
        return board.getPathWithCriteria(SpotType.values()[boardIndex]);
    }

    private ComputerPlayerProfile resolveProfile(SeatState seat) {
        if (seat.controllerProfileId() == null || seat.controllerProfileId().isBlank()) {
            return seat.seatKind() == SeatKind.BOT ? ComputerPlayerProfile.STRONG : ComputerPlayerProfile.HUMAN;
        }
        try {
            return ComputerPlayerProfile.valueOf(seat.controllerProfileId());
        } catch (IllegalArgumentException ignored) {
            return seat.seatKind() == SeatKind.BOT ? ComputerPlayerProfile.STRONG : ComputerPlayerProfile.HUMAN;
        }
    }

    private Color resolveColor(SeatState seat) {
        if (seat.tokenColorHex() == null || seat.tokenColorHex().isBlank()) {
            return switch (seat.seatIndex()) {
                case 0 -> Color.MEDIUMPURPLE;
                case 1 -> Color.PINK;
                case 2 -> Color.DARKOLIVEGREEN;
                case 3 -> Color.TURQUOISE;
                case 4 -> Color.MEDIUMBLUE;
                default -> Color.MEDIUMSPRINGGREEN;
            };
        }
        return Color.web(seat.tokenColorHex());
    }

    private int parsePlayerId(String playerId, int seatIndex) {
        if (playerId == null || !playerId.startsWith("player-")) {
            return seatIndex;
        }
        try {
            return Integer.parseInt(playerId.substring("player-".length()));
        } catch (NumberFormatException e) {
            return seatIndex;
        }
    }
}
