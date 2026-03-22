package fi.monopoly.components;

import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.support.TestObjectFactory;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerAssetTransferTest {

    @Test
    void addOwnedPropertyTransfersPropertyFromPreviousOwner() {
        Player firstOwner = TestObjectFactory.player("First", 300, 1);
        Player secondOwner = TestObjectFactory.player("Second", 300, 2);
        StreetProperty property = new StreetProperty(SpotType.B1);

        assertTrue(firstOwner.addOwnedProperty(property));
        assertSame(firstOwner, property.getOwnerPlayer());

        assertTrue(secondOwner.addOwnedProperty(property));
        assertTrue(firstOwner.getOwnedProperties().isEmpty());
        assertSame(secondOwner, property.getOwnerPlayer());
        assertTrue(secondOwner.getOwnedProperties().contains(property));
    }

    @Test
    void transferAssetsToMovesPropertiesCashAndCardsToCreditor() {
        Player debtor = TestObjectFactory.player("Debtor", 300, 1);
        Player creditor = TestObjectFactory.player("Creditor", 100, 2);
        StreetProperty mortgaged = new StreetProperty(SpotType.B1);
        mortgaged.setMortgaged(true);
        StreetProperty normal = new StreetProperty(SpotType.B2);
        TestObjectFactory.giveProperty(debtor, mortgaged);
        TestObjectFactory.giveProperty(debtor, normal);
        debtor.setGetOutOfJailCardCount(1);

        debtor.transferAssetsTo(creditor);

        assertTrue(debtor.getOwnedProperties().isEmpty());
        assertEquals(0, debtor.getMoneyAmount());
        assertSame(creditor, mortgaged.getOwnerPlayer());
        assertSame(creditor, normal.getOwnerPlayer());
        assertTrue(mortgaged.isMortgaged());
        assertEquals(397, creditor.getMoneyAmount());
        assertEquals(1, creditor.getGetOutOfJailCardCount());
    }

    @Test
    void releaseAssetsToBankClearsOwnershipAndUnmortgagesProperties() {
        Player debtor = TestObjectFactory.player("Debtor", 300, 1);
        StreetProperty property = new StreetProperty(SpotType.B1);
        property.setMortgaged(true);
        TestObjectFactory.giveProperty(debtor, property);

        debtor.releaseAssetsToBank();

        assertTrue(debtor.getOwnedProperties().isEmpty());
        assertEquals(0, debtor.getMoneyAmount());
        assertNull(property.getOwnerPlayer());
        assertFalse(property.isMortgaged());
    }
}
