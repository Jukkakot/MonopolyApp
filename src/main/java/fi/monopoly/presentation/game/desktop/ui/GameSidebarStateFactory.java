package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.components.Player;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.MonopolyUtils;

import java.util.List;

import static fi.monopoly.text.UiTexts.text;

public final class GameSidebarStateFactory {
    public GameSidebarPresenter.SidebarState createSidebarState(
            Player turnPlayer,
            List<Player> players,
            List<String> recentMessages,
            DebtState debtState,
            String persistenceNotice,
            boolean gameOver,
            boolean popupVisible,
            boolean animationsRunning,
            boolean endRoundVisible,
            boolean rollDiceVisible,
            SessionState authoritativeSessionState,
            float historyPanelY,
            float historyHeight,
            float reservedTop
    ) {
        String activePlayerSpotName = resolveActivePlayerSpotName(authoritativeSessionState);
        boolean activePlayerIsComputer = resolveActivePlayerIsComputer(authoritativeSessionState,
                turnPlayer != null && turnPlayer.isComputerControlled());
        return new GameSidebarPresenter.SidebarState(
                turnPlayer,
                resolveCurrentTurnPhase(gameOver, debtState, popupVisible, animationsRunning,
                        endRoundVisible, rollDiceVisible, authoritativeSessionState),
                players,
                recentMessages,
                debtState,
                persistenceNotice,
                historyPanelY,
                historyHeight,
                reservedTop,
                activePlayerSpotName,
                activePlayerIsComputer
        );
    }

    private static String resolveActivePlayerSpotName(SessionState state) {
        if (state == null || state.turn() == null || state.turn().activePlayerId() == null) {
            return null;
        }
        String activeId = state.turn().activePlayerId();
        return state.players().stream()
                .filter(p -> activeId.equals(p.playerId()))
                .findFirst()
                .map(p -> spotName(p.boardIndex()))
                .orElse(null);
    }

    private static boolean resolveActivePlayerIsComputer(SessionState state, boolean legacyFallback) {
        if (state == null || state.turn() == null || state.turn().activePlayerId() == null) {
            return legacyFallback;
        }
        String activeId = state.turn().activePlayerId();
        return state.seats().stream()
                .filter(s -> activeId.equals(s.playerId()))
                .findFirst()
                .map(s -> s.seatKind() == fi.monopoly.domain.session.SeatKind.BOT)
                .orElse(legacyFallback);
    }

    private static String spotName(int boardIndex) {
        List<SpotType> spots = SpotType.SPOT_TYPES;
        if (boardIndex < 0 || boardIndex >= spots.size()) {
            return null;
        }
        SpotType spotType = spots.get(boardIndex);
        String name = spotType.getStringProperty("name");
        return name.isBlank() ? spotType.name() : MonopolyUtils.parseIllegalCharacters(name);
    }

    public String resolveCurrentTurnPhase(
            boolean gameOver,
            DebtState debtState,
            boolean popupVisible,
            boolean animationsRunning,
            boolean endRoundVisible,
            boolean rollDiceVisible,
            SessionState authoritativeSessionState
    ) {
        if (gameOver) {
            return text("sidebar.phase.gameOver");
        }
        if (animationsRunning) {
            return text("sidebar.phase.animation");
        }
        // Prefer the authoritative domain phase when available; fall back to heuristics
        if (authoritativeSessionState != null && authoritativeSessionState.turn() != null) {
            TurnPhase phase = authoritativeSessionState.turn().phase();
            return switch (phase) {
                case WAITING_FOR_ROLL -> text("sidebar.phase.roll");
                case WAITING_FOR_END_TURN -> text("sidebar.phase.endTurn");
                case WAITING_FOR_DECISION -> text("sidebar.phase.popup");
                case RESOLVING_DEBT -> text("sidebar.phase.debt");
                case WAITING_FOR_AUCTION -> text("sidebar.phase.popup");
                case GAME_OVER -> text("sidebar.phase.gameOver");
                default -> text("sidebar.phase.resolving");
            };
        }
        // Legacy heuristic fallback
        if (debtState != null) {
            return text("sidebar.phase.debt");
        }
        if (popupVisible) {
            return text("sidebar.phase.popup");
        }
        if (endRoundVisible) {
            return text("sidebar.phase.endTurn");
        }
        if (rollDiceVisible) {
            return text("sidebar.phase.roll");
        }
        return text("sidebar.phase.resolving");
    }
}
