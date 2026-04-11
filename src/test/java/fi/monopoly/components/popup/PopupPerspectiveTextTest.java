package fi.monopoly.components.popup;

import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PopupPerspectiveTextTest {

    @Test
    void leavesHumanTurnTextUntouched() {
        Player human = new Player("Jukka", Color.BLUE, 1500, 1, ComputerPlayerProfile.HUMAN);

        assertEquals(
                "Do you want to buy it for M200?",
                PopupPerspectiveText.adaptForTurnPlayer("Do you want to buy it for M200?", human)
        );
    }

    @Test
    void replacesEnglishSecondPersonForBotTurns() {
        Player bot = new Player("Toka", Color.RED, 1500, 2, ComputerPlayerProfile.STRONG);

        assertEquals(
                "Toka arrived at WATER WORKS. Does Toka want to buy it for M150?",
                PopupPerspectiveText.adaptForTurnPlayer("Arrived at WATER WORKS. Do you want to buy it for M150?", bot)
        );
        assertEquals(
                "Toka was sent to jail",
                PopupPerspectiveText.adaptForTurnPlayer("You were sent to jail", bot)
        );
    }

    @Test
    void replacesFinnishSecondPersonForBotTurns() {
        Player bot = new Player("Kolmas", Color.GREEN, 1500, 3, ComputerPlayerProfile.STRONG);

        assertEquals(
                "Kolmas saapui ruutuun VENTNOR AVENUE. Haluaako pelaaja Kolmas ostaa sen hintaan M260?",
                PopupPerspectiveText.adaptForTurnPlayer("Saavuit ruutuun VENTNOR AVENUE. Haluatko ostaa sen hintaan M260?", bot)
        );
        assertEquals(
                "Pelaajan Kolmas taytyy maksaa M200 veroa.",
                PopupPerspectiveText.adaptForTurnPlayer("Sinun taytyy maksaa M200 veroa.", bot)
        );
    }
}
