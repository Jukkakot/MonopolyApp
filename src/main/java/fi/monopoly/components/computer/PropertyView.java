package fi.monopoly.components.computer;

import fi.monopoly.types.PlaceType;
import fi.monopoly.types.SpotType;
import fi.monopoly.types.StreetType;

public record PropertyView(
        SpotType spotType,
        StreetType streetType,
        PlaceType placeType,
        String name,
        int price,
        boolean mortgaged,
        int mortgageValue,
        int liquidationValue,
        int buildingLevel,
        int houseCount,
        int hotelCount,
        int rentEstimate,
        boolean completedSet
) {
}
