package fi.monopoly.presentation.game.desktop.session;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.computer.*;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.components.payment.PaymentRequest;
import fi.monopoly.components.popup.PopupService;
import fi.monopoly.components.properties.Property;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.components.spots.PropertySpot;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.components.board.Board;
import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class SessionViewFacade {
    private final PopupService popupService;
    private final Players players;
    private final Board board;
    private final Supplier<DebtState> debtStateSupplier;
    private final BooleanSupplier retryDebtVisibleSupplier;
    private final BooleanSupplier declareBankruptcyVisibleSupplier;
    private final Function<Player, Boolean> rollDiceAvailable;
    private final Function<Player, Boolean> endTurnAvailable;
    private final IntSupplier unownedPropertyCountSupplier;
    private final Function<Player, Integer> boardDangerScoreSupplier;

    private final Map<Integer, CachedPlayerView> playerViewCache = new HashMap<>();
    private CachedGameView cachedGameView;

    public SessionViewFacade(
            PopupService popupService,
            Players players,
            Board board,
            Supplier<DebtState> debtStateSupplier,
            BooleanSupplier retryDebtVisibleSupplier,
            BooleanSupplier declareBankruptcyVisibleSupplier,
            Function<Player, Boolean> rollDiceAvailable,
            Function<Player, Boolean> endTurnAvailable,
            IntSupplier unownedPropertyCountSupplier,
            Function<Player, Integer> boardDangerScoreSupplier
    ) {
        this.popupService = popupService;
        this.players = players;
        this.board = board;
        this.debtStateSupplier = debtStateSupplier;
        this.retryDebtVisibleSupplier = retryDebtVisibleSupplier;
        this.declareBankruptcyVisibleSupplier = declareBankruptcyVisibleSupplier;
        this.rollDiceAvailable = rollDiceAvailable;
        this.endTurnAvailable = endTurnAvailable;
        this.unownedPropertyCountSupplier = unownedPropertyCountSupplier;
        this.boardDangerScoreSupplier = boardDangerScoreSupplier;
    }

    public GameView createGameView(Player currentPlayer) {
        long gameViewSignature = buildGameViewSignature(currentPlayer);
        if (cachedGameView != null && cachedGameView.signature() == gameViewSignature) {
            return cachedGameView.view();
        }
        PopupView popupView = popupService.isAnyVisible()
                ? new PopupView(
                popupService.activePopupKind(),
                popupService.activePopupMessage(),
                popupService.activePopupActions(),
                createPopupPropertyView(currentPlayer)
        )
                : null;
        DebtState debtState = debtStateSupplier.get();
        DebtView debtView = debtState == null ? null : new DebtView(
                debtState.paymentRequest().amount(),
                debtState.paymentRequest().reason(),
                debtState.bankruptcyRisk(),
                debtState.paymentRequest().target().getClass().getSimpleName(),
                debtTargetName(debtState.paymentRequest())
        );
        List<PlayerView> playerViews = players.getPlayers().stream()
                .map(this::createPlayerView)
                .sorted(Comparator.comparingInt(PlayerView::turnNumber))
                .toList();
        GameView view = new GameView(
                currentPlayer.getId(),
                playerViews,
                new VisibleActionsView(
                        popupService.isAnyVisible(),
                        retryDebtVisibleSupplier.getAsBoolean(),
                        declareBankruptcyVisibleSupplier.getAsBoolean(),
                        rollDiceAvailable.apply(currentPlayer),
                        endTurnAvailable.apply(currentPlayer)
                ),
                popupView,
                debtView,
                unownedPropertyCountSupplier.getAsInt(),
                StreetProperty.BANK_HOUSE_SUPPLY - players.getTotalHouseCount(),
                StreetProperty.BANK_HOTEL_SUPPLY - players.getTotalHotelCount()
        );
        cachedGameView = new CachedGameView(gameViewSignature, view);
        return view;
    }

    public PlayerView createPlayerView(Player player) {
        long playerSignature = buildPlayerViewSignature(player);
        CachedPlayerView cachedPlayerView = playerViewCache.get(player.getId());
        if (cachedPlayerView != null && cachedPlayerView.signature() == playerSignature) {
            return cachedPlayerView.view();
        }
        List<Property> ownedPlayerProperties = player.getOwnedProperties();
        List<PropertyView> ownedProperties = ownedPlayerProperties.stream()
                .map(property -> createPropertyView(player, property))
                .sorted(Comparator.comparing(property -> property.spotType().ordinal()))
                .toList();
        List<StreetType> completedSets = ownedPlayerProperties.stream()
                .map(Property::getSpotType)
                .map(spotType -> spotType.streetType)
                .distinct()
                .filter(player::ownsAllStreetProperties)
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .toList();
        PlayerView playerView = new PlayerView(
                player.getId(),
                player.getName(),
                player.getMoneyAmount(),
                player.getTurnNumber(),
                player.getComputerProfile(),
                player.getSpot().getSpotType(),
                player.isInJail(),
                JailSpot.jailTimeLeftMap.getOrDefault(player, 0),
                player.getGetOutOfJailCardCount(),
                player.getTotalHouseCount(),
                player.getTotalHotelCount(),
                player.getTotalLiquidationValue(),
                boardDangerScoreSupplier.apply(player),
                completedSets,
                ownedProperties
        );
        playerViewCache.put(player.getId(), new CachedPlayerView(playerSignature, playerView));
        return playerView;
    }

    private long buildGameViewSignature(Player currentPlayer) {
        long signature = currentPlayer == null ? 0 : currentPlayer.getId();
        signature = signature * 31 + Boolean.hashCode(popupService.isAnyVisible());
        if (popupService.isAnyVisible()) {
            signature = signature * 31 + String.valueOf(popupService.activePopupKind()).hashCode();
            signature = signature * 31 + String.valueOf(popupService.activePopupMessage()).hashCode();
            signature = signature * 31 + popupService.activePopupActions().hashCode();
        }
        signature = signature * 31 + Boolean.hashCode(retryDebtVisibleSupplier.getAsBoolean());
        signature = signature * 31 + Boolean.hashCode(declareBankruptcyVisibleSupplier.getAsBoolean());
        signature = signature * 31 + Boolean.hashCode(rollDiceAvailable.apply(currentPlayer));
        signature = signature * 31 + Boolean.hashCode(endTurnAvailable.apply(currentPlayer));
        signature = signature * 31 + unownedPropertyCountSupplier.getAsInt();
        signature = signature * 31 + (StreetProperty.BANK_HOUSE_SUPPLY - players.getTotalHouseCount());
        signature = signature * 31 + (StreetProperty.BANK_HOTEL_SUPPLY - players.getTotalHotelCount());
        DebtState debtState = debtStateSupplier.get();
        if (debtState != null) {
            signature = signature * 31 + debtState.paymentRequest().amount();
            signature = signature * 31 + debtState.paymentRequest().reason().hashCode();
            signature = signature * 31 + Boolean.hashCode(debtState.bankruptcyRisk());
        }
        Property offeredProperty = popupService.activeOfferedProperty();
        if (offeredProperty != null) {
            signature = signature * 31 + offeredProperty.getSpotType().ordinal();
        }
        for (Player player : players.getPlayers()) {
            signature = signature * 31 + buildPlayerViewSignature(player);
        }
        return signature;
    }

    private long buildPlayerViewSignature(Player player) {
        long signature = player.getId();
        signature = signature * 31 + player.getMoneyAmount();
        signature = signature * 31 + player.getTurnNumber();
        signature = signature * 31 + player.getSpot().getSpotType().ordinal();
        signature = signature * 31 + Boolean.hashCode(player.isInJail());
        signature = signature * 31 + JailSpot.jailTimeLeftMap.getOrDefault(player, 0);
        signature = signature * 31 + player.getGetOutOfJailCardCount();
        signature = signature * 31 + player.getTotalHouseCount();
        signature = signature * 31 + player.getTotalHotelCount();
        signature = signature * 31 + player.getTotalLiquidationValue();
        signature = signature * 31 + boardDangerScoreSupplier.apply(player);
        for (Property property : player.getOwnedProperties()) {
            signature = signature * 31 + property.getSpotType().ordinal();
            signature = signature * 31 + property.getPrice();
            signature = signature * 31 + Boolean.hashCode(property.isMortgaged());
            signature = signature * 31 + property.getLiquidationValue();
            if (property instanceof StreetProperty streetProperty) {
                signature = signature * 31 + streetProperty.getHousePrice();
                signature = signature * 31 + streetProperty.getBuildingLevel();
                signature = signature * 31 + streetProperty.getHouseCount();
                signature = signature * 31 + streetProperty.getHotelCount();
            }
        }
        return signature;
    }

    private PropertyView createPropertyView(Player owner, Property property) {
        int housePrice = 0;
        int buildingLevel = 0;
        int houseCount = 0;
        int hotelCount = 0;
        if (property instanceof StreetProperty streetProperty) {
            housePrice = streetProperty.getHousePrice();
            buildingLevel = streetProperty.getBuildingLevel();
            houseCount = streetProperty.getHouseCount();
            hotelCount = streetProperty.getHotelCount();
        }
        return new PropertyView(
                property.getSpotType(),
                property.getSpotType().streetType,
                property.getSpotType().streetType.placeType,
                property.getDisplayName(),
                property.getPrice(),
                property.isMortgaged(),
                property.getMortgageValue(),
                property.getLiquidationValue(),
                housePrice,
                buildingLevel,
                houseCount,
                hotelCount,
                estimateRent(property, owner),
                owner.ownsAllStreetProperties(property.getSpotType().streetType)
        );
    }

    private PropertyView createPopupPropertyView(Player currentPlayer) {
        Property offeredProperty = popupService.activeOfferedProperty();
        if (offeredProperty == null) {
            return null;
        }
        return new PropertyView(
                offeredProperty.getSpotType(),
                offeredProperty.getSpotType().streetType,
                offeredProperty.getSpotType().streetType.placeType,
                offeredProperty.getDisplayName(),
                offeredProperty.getPrice(),
                offeredProperty.isMortgaged(),
                offeredProperty.getMortgageValue(),
                offeredProperty.getLiquidationValue(),
                0,
                0,
                0,
                0,
                estimateOfferedPropertyRent(offeredProperty, currentPlayer),
                currentPlayer != null && currentPlayer.countOwnedProperties(offeredProperty.getSpotType().streetType) + 1
                        >= SpotType.getNumberOfSpots(offeredProperty.getSpotType().streetType)
        );
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

    private int estimateOfferedPropertyRent(Property property, Player currentPlayer) {
        if (property.getSpotType().streetType.placeType == PlaceType.UTILITY) {
            int utilityCount = currentPlayer == null ? 0 : currentPlayer.countOwnedProperties(property.getSpotType().streetType) + 1;
            return utilityCount >= 2 ? 70 : 28;
        }
        Player simulatedVisitor = null;
        for (Player candidate : players.getPlayers()) {
            if (candidate != currentPlayer) {
                simulatedVisitor = candidate;
                break;
            }
        }
        if (simulatedVisitor == null) {
            return 0;
        }
        Player originalOwner = property.getOwnerPlayer();
        property.setOwnerPlayer(currentPlayer);
        try {
            return property.getRent(simulatedVisitor);
        } finally {
            property.setOwnerPlayer(originalOwner);
        }
    }

    private String debtTargetName(PaymentRequest paymentRequest) {
        if (paymentRequest.target() instanceof fi.monopoly.components.payment.PlayerTarget playerTarget) {
            return playerTarget.player().getName();
        }
        return paymentRequest.target().getClass().getSimpleName();
    }

    private record CachedGameView(long signature, GameView view) {
    }

    private record CachedPlayerView(long signature, PlayerView view) {
    }
}
