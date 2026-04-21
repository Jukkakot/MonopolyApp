package fi.monopoly.infrastructure.persistence.session;

import controlP5.ControlP5;
import fi.monopoly.MonopolyApp;
import fi.monopoly.client.desktop.MonopolyRuntime;
import fi.monopoly.components.Player;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.components.properties.PropertyFactory;
import fi.monopoly.components.properties.StreetProperty;
import fi.monopoly.components.spots.JailSpot;
import fi.monopoly.domain.session.ControlMode;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.PropertyStateSnapshot;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SeatState;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import fi.monopoly.types.SpotType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import processing.awt.PGraphicsJava2D;
import processing.core.PFont;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacySessionRuntimeRestorerTest {
    private final LegacySessionRuntimeRestorer restorer = new LegacySessionRuntimeRestorer();

    @AfterEach
    void tearDown() {
        PropertyFactory.resetState();
        JailSpot.jailTimeLeftMap.clear();
    }

    @Test
    void restoresPlayersPropertiesAndTurnFromAuthoritativeSessionState() {
        MonopolyRuntime runtime = initHeadlessRuntime();
        SessionState state = new SessionState(
                "session-1",
                12L,
                SessionStatus.IN_PROGRESS,
                List.of(
                        new SeatState("seat-0", 0, "player-5", SeatKind.HUMAN, ControlMode.MANUAL, "Eka", "HUMAN", "#9370DB"),
                        new SeatState("seat-1", 1, "player-7", SeatKind.BOT, ControlMode.AUTOPLAY, "Toka", "STRONG", "#FFC0CB")
                ),
                List.of(
                        new PlayerSnapshot("player-5", "seat-0", "Eka", 1325, SpotType.B1.ordinal(), false, false, false, 0, 1, List.of("B1")),
                        new PlayerSnapshot("player-7", "seat-1", "Toka", 980, SpotType.JAIL.ordinal(), false, false, true, 2, 0, List.of("RR1"))
                ),
                List.of(
                        new PropertyStateSnapshot("B1", "player-5", false, 2, 0),
                        new PropertyStateSnapshot("RR1", "player-7", true, 0, 0)
                ),
                new TurnState("player-7", TurnPhase.WAITING_FOR_DECISION, false, true),
                null,
                null,
                null,
                null,
                null
        );

        RestoredLegacySessionRuntime restored = restorer.restore(runtime, state);

        Player human = restored.playersById().get("player-5");
        Player bot = restored.playersById().get("player-7");
        StreetProperty mediterranean = (StreetProperty) PropertyFactory.getProperty(SpotType.B1);

        assertEquals(2, restored.players().count());
        assertEquals("Toka", restored.players().getTurn().getName());
        assertEquals(1325, human.getMoneyAmount());
        assertEquals(ComputerPlayerProfile.STRONG, bot.getComputerProfile());
        assertEquals(SpotType.B1, human.getSpot().getSpotType());
        assertTrue(bot.isInJail());
        assertEquals(2, JailSpot.jailTimeLeftMap.get(bot));
        assertSame(human, mediterranean.getOwnerPlayer());
        assertEquals(2, mediterranean.getHouseCount());
        assertTrue(bot.getOwnedProperties().contains(PropertyFactory.getProperty(SpotType.RR1)));
    }

    private static MonopolyRuntime initHeadlessRuntime() {
        MonopolyApp app = new MonopolyApp();
        app.width = MonopolyApp.DEFAULT_WINDOW_WIDTH;
        app.height = MonopolyApp.DEFAULT_WINDOW_HEIGHT;

        PGraphicsJava2D graphics = new PGraphicsJava2D();
        graphics.setParent(app);
        graphics.setPrimary(true);
        graphics.setSize(app.width, app.height);
        app.g = graphics;

        ControlP5 controlP5 = new ControlP5(app);
        PFont font = app.createFont("Arial", 20);
        return MonopolyRuntime.initialize(app, controlP5, font, font, font);
    }
}
