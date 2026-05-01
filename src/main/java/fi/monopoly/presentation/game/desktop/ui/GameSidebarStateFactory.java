package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.components.Player;
import fi.monopoly.components.payment.DebtState;
import fi.monopoly.domain.session.PlayerSnapshot;
import fi.monopoly.domain.session.SessionState;
import fi.monopoly.domain.turn.TurnPhase;
import fi.monopoly.types.SpotType;
import fi.monopoly.utils.MonopolyUtils;

import java.util.List;
import java.util.Map;

import static fi.monopoly.text.UiTexts.text;

public final class GameSidebarStateFactory {
    public GameSidebarPresenter.SidebarState createSidebarState(
            Player turnPlayer,
            List<Player> legacyPlayers,
            List<String> recentMessages,
            DebtState debtState,
            String persistenceNotice,
            boolean animationsRunning,
            SessionState authoritativeSessionState,
            float historyPanelY,
            float historyHeight,
            float reservedTop
    ) {
        String activePlayerSpotName = resolveActivePlayerSpotName(authoritativeSessionState);
        boolean activePlayerIsComputer = resolveActivePlayerIsComputer(authoritativeSessionState,
                turnPlayer != null && turnPlayer.isComputerControlled());
        String activePlayerName = resolveActivePlayerName(authoritativeSessionState,
                turnPlayer != null ? turnPlayer.getName() : null);
        Map<String, int[]> playerColors = resolvePlayerColors(authoritativeSessionState, legacyPlayers);
        return new GameSidebarPresenter.SidebarState(
                turnPlayer,
                resolveCurrentTurnPhase(animationsRunning, authoritativeSessionState),
                playerColors,
                recentMessages,
                debtState,
                persistenceNotice,
                historyPanelY,
                historyHeight,
                reservedTop,
                activePlayerSpotName,
                activePlayerIsComputer,
                activePlayerName
        );
    }

    private static Map<String, int[]> resolvePlayerColors(SessionState state, List<Player> legacyFallback) {
        if (state != null && !state.seats().isEmpty()) {
            Map<String, int[]> result = new java.util.LinkedHashMap<>();
            for (fi.monopoly.domain.session.SeatState seat : state.seats()) {
                String hex = seat.tokenColorHex();
                if (hex != null && hex.length() == 7 && hex.startsWith("#")) {
                    try {
                        int r = Integer.parseInt(hex.substring(1, 3), 16);
                        int g = Integer.parseInt(hex.substring(3, 5), 16);
                        int b = Integer.parseInt(hex.substring(5, 7), 16);
                        result.put(seat.displayName(), new int[]{r, g, b});
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (!result.isEmpty()) {
                return result;
            }
        }
        if (legacyFallback == null) {
            return Map.of();
        }
        Map<String, int[]> result = new java.util.LinkedHashMap<>();
        for (Player p : legacyFallback) {
            javafx.scene.paint.Color c = p.getColor();
            if (c != null) {
                result.put(p.getName(), new int[]{
                        (int) Math.round(c.getRed() * 255),
                        (int) Math.round(c.getGreen() * 255),
                        (int) Math.round(c.getBlue() * 255)
                });
            }
        }
        return result;
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

    private static String resolveActivePlayerName(SessionState state, String legacyFallback) {
        if (state == null || state.turn() == null || state.turn().activePlayerId() == null) {
            return legacyFallback;
        }
        String activeId = state.turn().activePlayerId();
        return state.players().stream()
                .filter(p -> activeId.equals(p.playerId()))
                .findFirst()
                .map(PlayerSnapshot::name)
                .orElse(legacyFallback);
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

    public String resolveCurrentTurnPhase(boolean animationsRunning, SessionState authoritativeSessionState) {
        if (animationsRunning) {
            return text("sidebar.phase.animation");
        }
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
        return text("sidebar.phase.resolving");
    }
}
