package fi.monopoly.application.session;

import fi.monopoly.application.command.RefreshSessionViewCommand;
import fi.monopoly.domain.session.*;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.domain.turn.TurnState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SessionApplicationServiceTest {

    @Test
    void refreshCommandReturnsAcceptedProjectedState() {
        SessionState expectedState = sampleState();
        SessionApplicationService service = new SessionApplicationService("local-session", () -> expectedState);

        var result = service.handle(new RefreshSessionViewCommand("local-session"));

        assertTrue(result.accepted());
        assertEquals(expectedState, result.sessionState());
        assertTrue(result.rejections().isEmpty());
    }

    @Test
    void refreshCommandRejectsWrongSessionId() {
        SessionApplicationService service = new SessionApplicationService("local-session", this::sampleState);

        var result = service.handle(new RefreshSessionViewCommand("other-session"));

        assertFalse(result.accepted());
        assertEquals(1, result.rejections().size());
        assertEquals("WRONG_SESSION", result.rejections().get(0).code());
    }

    private SessionState sampleState() {
        return new SessionState(
                "local-session",
                0L,
                SessionStatus.IN_PROGRESS,
                List.of(new SeatState("seat-0", 0, "player-0", SeatKind.HUMAN, ControlMode.MANUAL, "Human", "HUMAN", "#000000")),
                List.of(new PlayerSnapshot("player-0", "seat-0", "Human", 1500, -1, false, false, false, 0, 0, List.of())),
                List.of(),
                new TurnState("player-0", TurnPhase.WAITING_FOR_ROLL, true, false),
                null,
                null,
                null,
                null,
                null
        );
    }
}
