package fi.monopoly.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StreetTypeTest {

    @Test
    void streetTypesExposeExpectedPlaceTypes() {
        assertEquals(PlaceType.STREET, StreetType.BROWN.placeType);
        assertEquals(PlaceType.RAILROAD, StreetType.RAILROAD.placeType);
        assertEquals(PlaceType.UTILITY, StreetType.UTILITY.placeType);
        assertEquals(PlaceType.CORNER, StreetType.CORNER.placeType);
    }

    @Test
    void streetTypesContainEitherColorOrImageMetadata() {
        assertNotNull(StreetType.BROWN.color);
        assertNotNull(StreetType.RAILROAD.imgName);
        assertNotNull(StreetType.CHANCE.imgName);
    }
}
