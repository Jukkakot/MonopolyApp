package fi.monopoly.components.computer;

import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;

import java.util.List;

public record PlayerView(
        int id,
        String name,
        int moneyAmount,
        int turnNumber,
        ComputerPlayerProfile computerProfile,
        SpotType currentSpot,
        boolean inJail,
        int jailRoundsLeft,
        int getOutOfJailCardCount,
        int totalHouseCount,
        int totalHotelCount,
        int totalLiquidationValue,
        int boardDangerScore,
        List<StreetType> completedSets,
        List<PropertyView> ownedProperties
) {
    public PlayerView {
        completedSets = List.copyOf(completedSets);
        ownedProperties = List.copyOf(ownedProperties);
    }
}
