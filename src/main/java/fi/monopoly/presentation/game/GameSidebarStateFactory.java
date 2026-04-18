package fi.monopoly.presentation.game;

import fi.monopoly.components.Player;
import fi.monopoly.components.payment.DebtState;

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
            float historyPanelY,
            float historyHeight,
            float reservedTop
    ) {
        return new GameSidebarPresenter.SidebarState(
                turnPlayer,
                resolveCurrentTurnPhase(gameOver, debtState, popupVisible, animationsRunning, endRoundVisible, rollDiceVisible),
                players,
                recentMessages,
                debtState,
                persistenceNotice,
                historyPanelY,
                historyHeight,
                reservedTop
        );
    }

    public String resolveCurrentTurnPhase(
            boolean gameOver,
            DebtState debtState,
            boolean popupVisible,
            boolean animationsRunning,
            boolean endRoundVisible,
            boolean rollDiceVisible
    ) {
        if (gameOver) {
            return text("sidebar.phase.gameOver");
        }
        if (debtState != null) {
            return text("sidebar.phase.debt");
        }
        if (popupVisible) {
            return text("sidebar.phase.popup");
        }
        if (animationsRunning) {
            return text("sidebar.phase.animation");
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
