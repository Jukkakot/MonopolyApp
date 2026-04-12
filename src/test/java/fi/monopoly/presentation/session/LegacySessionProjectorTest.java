package fi.monopoly.presentation.session;

import fi.monopoly.components.Player;
import fi.monopoly.components.Players;
import fi.monopoly.components.computer.ComputerPlayerProfile;
import fi.monopoly.domain.decision.DecisionAction;
import fi.monopoly.domain.decision.DecisionType;
import fi.monopoly.domain.session.SeatKind;
import fi.monopoly.domain.session.SessionStatus;
import fi.monopoly.domain.turn.TurnPhase;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacySessionProjectorTest {

    @Test
    void mapsPausedGameToPausedSessionStatus() {
        LegacySessionProjector projector = projector(null, () -> true, () -> false, () -> false, () -> true, () -> false);
        assertEquals(SessionStatus.PAUSED, projector.project().status());
    }

    @Test
    void mapsGameOverAndWinnerIntoSessionSnapshot() {
        Players players = samplePlayers();
        Player winner = players.getPlayers().get(1);
        LegacySessionProjector projector = new LegacySessionProjector(
                "local-session",
                () -> players,
                () -> null,
                () -> null,
                () -> false,
                () -> true,
                () -> winner,
                () -> false,
                () -> false
        );

        var state = projector.project();
        assertEquals(SessionStatus.GAME_OVER, state.status());
        assertEquals("player-" + winner.getId(), state.winnerPlayerId());
        assertEquals(TurnPhase.GAME_OVER, state.turn().phase());
    }

    @Test
    void mapsHumanAndBotSeats() {
        var state = projector(null, () -> false, () -> false, () -> true, () -> true, () -> false).project();
        assertEquals(SeatKind.HUMAN, state.seats().get(0).seatKind());
        assertEquals(SeatKind.BOT, state.seats().get(1).seatKind());
    }

    @Test
    void mapsPropertyOfferPopupToPropertyPurchasePendingDecision() {
        var popup = new LegacyPopupSnapshot("propertyOffer", "Buy Boardwalk?", List.of("Buy", "Decline"));
        var state = projector(popup, () -> false, () -> false, () -> false, () -> false, () -> false).project();
        assertNotNull(state.pendingDecision());
        assertEquals(DecisionType.PROPERTY_PURCHASE, state.pendingDecision().decisionType());
        assertEquals(List.of(DecisionAction.BUY_PROPERTY, DecisionAction.DECLINE_PROPERTY), state.pendingDecision().allowedActions());
        assertNull(state.pendingDecision().payload());
        assertEquals(TurnPhase.WAITING_FOR_DECISION, state.turn().phase());
    }

    @Test
    void mapsActiveDebtToResolvingDebtPhase() {
        var state = projector(null, () -> false, () -> true, () -> false, () -> false, () -> false).project();
        assertEquals(TurnPhase.RESOLVING_DEBT, state.turn().phase());
    }

    private LegacySessionProjector projector(
            LegacyPopupSnapshot popup,
            java.util.function.BooleanSupplier paused,
            java.util.function.BooleanSupplier debtActive,
            java.util.function.BooleanSupplier gameOver,
            java.util.function.BooleanSupplier canRoll,
            java.util.function.BooleanSupplier canEndTurn
    ) {
        Players players = samplePlayers();
        return new LegacySessionProjector(
                "local-session",
                () -> players,
                () -> popup,
                () -> debtActive.getAsBoolean() ? new fi.monopoly.components.payment.DebtState(null, null, true) : null,
                paused,
                gameOver,
                () -> null,
                canRoll,
                canEndTurn
        );
    }

    private Players samplePlayers() {
        Players players = new Players(null);
        players.addPlayer(new Player("Human", Color.MEDIUMPURPLE, 1500, 1, ComputerPlayerProfile.HUMAN));
        players.addPlayer(new Player("Bot", Color.PINK, 1400, 2, ComputerPlayerProfile.STRONG));
        return players;
    }
}
