package fi.monopoly.types;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TurnResultTest {

    @Test
    void copyOfReturnsNullForNullInput() {
        assertNull(TurnResult.copyOf(null));
    }

    @Test
    void copyOfCreatesSeparateInstanceWithSameValues() {
        TurnResult original = TurnResult.builder()
                .nextSpotCriteria(SpotType.JAIL)
                .pathMode(PathMode.FLY)
                .shouldGoToJail(true)
                .build();

        TurnResult copy = TurnResult.copyOf(original);

        assertNotNull(copy);
        assertNotSame(original, copy);
        assertEquals(original.getNextSpotCriteria(), copy.getNextSpotCriteria());
        assertEquals(original.getPathMode(), copy.getPathMode());
        assertTrue(copy.isShouldGoToJail());
    }
}
