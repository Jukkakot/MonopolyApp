package fi.monopoly.components.properties;

import fi.monopoly.components.Player;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailRoadPropertyTest {

    @Test
    void rentScalesWithNumberOfOwnedRailroads() {
        Player owner = TestObjectFactory.player("Owner", 1500, 1);
        Player visitor = TestObjectFactory.player("Visitor", 1500, 2);
        RailRoadProperty first = new RailRoadProperty(SpotType.RR1);
        RailRoadProperty second = new RailRoadProperty(SpotType.RR2);

        TestObjectFactory.giveProperty(owner, first);
        assertEquals(25, first.getRent(visitor));

        TestObjectFactory.giveProperty(owner, second);
        assertEquals(50, first.getRent(visitor));
    }
}
