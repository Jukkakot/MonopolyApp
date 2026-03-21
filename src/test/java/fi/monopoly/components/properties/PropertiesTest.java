package fi.monopoly.components.properties;

import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PropertiesTest {

    @Test
    void addPropertyRejectsDuplicates() {
        Properties properties = new Properties();
        Property property = new StreetProperty(SpotType.B1);

        assertTrue(properties.addProperty(property));
        assertFalse(properties.addProperty(property));
        assertEquals(1, properties.getProperties().size());
    }

    @Test
    void totalHouseAndHotelCountsAreSummedFromStreetProperties() {
        Properties properties = new Properties();
        StreetProperty first = new StreetProperty(SpotType.B1);
        StreetProperty second = new StreetProperty(SpotType.LB1);

        setBuildings(first, 2, 0);
        setBuildings(second, 0, 1);

        properties.addProperty(first);
        properties.addProperty(second);

        assertEquals(2, properties.getTotalHouseCount());
        assertEquals(1, properties.getTotalHotelCount());
    }

    private void setBuildings(StreetProperty property, int houseCount, int hotelCount) {
        try {
            var houseField = StreetProperty.class.getDeclaredField("houseCount");
            houseField.setAccessible(true);
            houseField.set(property, houseCount);

            var hotelField = StreetProperty.class.getDeclaredField("hotelCount");
            hotelField.setAccessible(true);
            hotelField.set(property, hotelCount);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
