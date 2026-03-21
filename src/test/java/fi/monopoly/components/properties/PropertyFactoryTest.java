package fi.monopoly.components.properties;

import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class PropertyFactoryTest {

    @Test
    void getPropertyReturnsExpectedSubclassAndCachesInstance() {
        Property street = PropertyFactory.getProperty(SpotType.B1);
        Property railroad = PropertyFactory.getProperty(SpotType.RR1);
        Property utility = PropertyFactory.getProperty(SpotType.U1);

        assertInstanceOf(StreetProperty.class, street);
        assertInstanceOf(RailRoadProperty.class, railroad);
        assertInstanceOf(UtilityProperty.class, utility);
        assertSame(street, PropertyFactory.getProperty(SpotType.B1));
    }
}
