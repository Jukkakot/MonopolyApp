package fi.monopoly.presentation.game.desktop.ui;

import fi.monopoly.client.desktop.DesktopClientSettings;
import fi.monopoly.components.board.Board;
import fi.monopoly.components.dices.DiceValue;
import fi.monopoly.components.dices.Dices;
import fi.monopoly.components.spots.Spot;
import fi.monopoly.presentation.game.desktop.runtime.DebugController;
import fi.monopoly.presentation.game.turn.GameTurnFlowCoordinator;
import fi.monopoly.presentation.session.debt.DebtController;
import fi.monopoly.presentation.session.trade.TradeController;
import fi.monopoly.types.DiceState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BooleanSupplier;

@Slf4j
@RequiredArgsConstructor
public final class GameUiHooksAdapter implements GameUiController.Hooks {
    private final Board board;
    private final Dices dices;
    private final DebtController debtController;
    private final TradeController tradeController;
    private final DebugController debugController;
    private final GameTurnFlowCoordinator gameTurnFlowCoordinator;
    private final Runnable payDebtAction;
    private final Runnable declareBankruptcyAction;
    private final Runnable finishAllAnimationsAction;
    private final BooleanSupplier gameOverSupplier;
    private final BooleanSupplier popupVisibleSupplier;
    private final BooleanSupplier canEndTurnSupplier;

    @Override
    public boolean gameOver() {
        return gameOverSupplier.getAsBoolean();
    }

    @Override
    public boolean popupVisible() {
        return popupVisibleSupplier.getAsBoolean();
    }

    @Override
    public boolean debtActive() {
        return debtController.debtState() != null;
    }

    @Override
    public boolean canEndTurn() {
        return canEndTurnSupplier.getAsBoolean();
    }

    @Override
    public void openTradeMenu() {
        tradeController.openTradeMenu();
    }

    @Override
    public void payDebt() {
        payDebtAction.run();
    }

    @Override
    public void declareBankruptcy() {
        declareBankruptcyAction.run();
    }

    @Override
    public void endRound() {
        if (!popupVisible() && debtController.debtState() == null) {
            gameTurnFlowCoordinator.endRound(true);
        }
    }

    @Override
    public void openGodModeMenu() {
        debugController.openGodModeMenu();
    }

    @Override
    public void finishAllAnimations() {
        finishAllAnimationsAction.run();
    }

    @Override
    public void toggleSkipAnimations() {
        DesktopClientSettings.toggleSkipAnimations();
        log.debug("Skip animations: {}", DesktopClientSettings.skipAnimations());
    }

    @Override
    public Spot hoveredSpot() {
        return board.getHoveredSpot();
    }

    @Override
    public boolean debugFlyToHoveredSpot(Spot hoveredSpot) {
        if (!dices.isVisible()) {
            return false;
        }
        dices.setValue(new DiceValue(DiceState.DEBUG_REROLL, 8));
        boolean played = gameTurnFlowCoordinator.playDebugRound(hoveredSpot, DiceState.DEBUG_REROLL);
        if (played) {
            dices.hide();
        }
        return played;
    }

}
