package fi.monopoly.presentation.game;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.TradeStatus;
import fi.monopoly.types.PlaceType;

public final class GameSessionQueries {
    private final Players players;
    private final Board board;

    public GameSessionQueries(Players players, Board board) {
        this.players = players;
        this.board = board;
    }

    public Player findPlayerById(String playerId) {
        if (playerId == null) {
            return null;
        }
        for (Player player : players.getPlayers()) {
            if (playerId.equals("player-" + player.getId())) {
                return player;
            }
        }
        return null;
    }

    public String resolveTradeActorId(SessionState sessionState) {
        if (sessionState.tradeState() == null) {
            return null;
        }
        if (sessionState.tradeState().status() == TradeStatus.EDITING) {
            return sessionState.tradeState().editingPlayerId();
        }
        return sessionState.tradeState().decisionRequiredFromPlayerId();
    }

    public int countUnownedProperties() {
        int unownedProperties = 0;
        for (Spot spot : board.getSpots()) {
            if (spot instanceof PropertySpot propertySpot && !propertySpot.getProperty().hasOwner()) {
                unownedProperties++;
            }
        }
        return unownedProperties;
    }

    public int calculateBoardDangerScore(Player player) {
        int boardDangerScore = 0;
        for (Spot spot : board.getSpots()) {
            if (!(spot instanceof PropertySpot propertySpot)) {
                continue;
            }
            Property property = propertySpot.getProperty();
            if (!property.hasOwner() || !property.isNotOwner(player)) {
                continue;
            }
            boardDangerScore += calculateDangerRent(property);
        }
        return boardDangerScore;
    }

    private int calculateDangerRent(Property property) {
        Player owner = property.getOwnerPlayer();
        if (owner == null) {
            return 0;
        }
        return switch (property.getSpotType().streetType.placeType) {
            case UTILITY -> owner.countOwnedProperties(property.getSpotType().streetType) >= 2 ? 70 : 28;
            case RAILROAD, STREET -> estimateRent(property, owner);
            default -> 0;
        };
    }

    private int estimateRent(Property property, Player owner) {
        if (property.getSpotType().streetType.placeType == PlaceType.UTILITY) {
            return switch (owner.countOwnedProperties(property.getSpotType().streetType)) {
                case 2 -> 70;
                default -> 28;
            };
        }
        Player nonOwner = null;
        for (Player candidate : players.getPlayers()) {
            if (candidate != owner) {
                nonOwner = candidate;
                break;
            }
        }
        return nonOwner == null ? 0 : property.getRent(nonOwner);
    }
}
